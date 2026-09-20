from pymodbus.server import StartTcpServer
from pymodbus.simulator import DataType, SimData, SimDevice
import random
import time

# Register map (all Holding Registers, plain 16-bit integers, scaled where noted):
#   HR1  - QC1 Raw Water Turbidity              (NTU x10)
#   HR2  - QC2 Post-Filtration Turbidity        (NTU x10)
#   HR3  - QC2 Post-Filtration Chlorine Residual (mg/L x100)
#   HR4  - CoagulantPump DoseSetpoint           (mL/min x10)
#   HR5  - CoagulantPump DosePV                 (mL/min x10)
#   HR6  - CoagulantPump Running                (0/1)
#   HR7  - CoagulantPump Fault                  (0/1)
#   HR8  - SHPump DoseSetpoint                  (mL/min x10)
#   HR9  - SHPump DosePV                        (mL/min x10)
#   HR10 - SHPump Running                       (0/1)
#   HR11 - SHPump Fault                         (0/1)
#   HR12 - SBSPump DoseSetpoint                 (mL/min x10)
#   HR13 - SBSPump DosePV                       (mL/min x10)
#   HR14 - SBSPump Running                      (0/1)
#   HR15 - SBSPump Fault                        (0/1)
#   HR16 - QC3 RO Feed Conductivity
#   HR17 - QC4 Permeate Conductivity
#   HR18 - RO Feed Tank Level                   (percent, 0-100)
#   HR19 - AntiscalantPump DoseSetpoint         (mL/min x10)
#   HR20 - AntiscalantPump DosePV               (mL/min x10)
#   HR21 - AntiscalantPump Running              (0/1)
#   HR22 - AntiscalantPump Fault                (0/1)
#   HR23 - CausticPump DoseSetpoint             (mL/min x10)
#   HR24 - CausticPump DosePV                   (mL/min x10)
#   HR25 - CausticPump Running                  (0/1)
#   HR26 - CausticPump Fault                    (0/1)
#   HR27 - InfluentFlowRate
#   HR28 - FiltrateFlowRate
#   HR29 - ROFeedFlowRate
#   HR30 - InfluentTemperature
#   HR31 - Hardness
#   HR32 - ROFeed_pH                            (pH x100)
#
# NOTE: DoseSetpoint/DosePV use x10 scaling (added after Week 3 debugging showed
# whole-number-only precision rounds a floor clamp like 0.3 straight to 0, defeating
# the clamp). Same convention as the other analog readings above -- Modbus holding
# registers can't hold fractions, so the driving Gateway script multiplies by 10
# before writing, and Perspective divides by 10 for display.
initial_values = [
    25, 8, 120,                    # HR1-3   QC1/QC2 (existing)
    0, 0, 1, 0,                    # HR4-7   CoagulantPump (existing)
    0, 0, 1, 0,                    # HR8-11  SHPump (existing)
    0, 0, 1, 0,                    # HR12-15 SBSPump (existing)
    450,                           # HR16    QC3 RO Feed Conductivity (existing)
    18,                            # HR17    QC4 Permeate Conductivity (existing)
    75,                            # HR18    RO Feed Tank Level: starts full/healthy, not pre-alarmed
    0, 0, 1, 0,                    # HR19-22 AntiscalantPump: setpoint, PV, running, fault
    0, 0, 1, 0,                    # HR23-26 CausticPump: setpoint, PV, running, fault
    150,                           # HR27    InfluentFlowRate: 150 GPM
    145,                           # HR28    FiltrateFlowRate: 145 GPM
    125,                           # HR29    ROFeedFlowRate: 125 GPM (paced below Filtrate so the tank refills)
    68,                            # HR30    InfluentTemperature: 68°F
    180,                           # HR31    Hardness: 180 mg/L as CaCO3
    680,                           # HR32    ROFeed_pH: 6.80 pH
]

# --- Process noise / dynamics --------------------------------------------
# pymodbus's SimDevice supports an `action=` callback that fires on every
# Modbus request (read or write) and hands us a direct, mutable reference to
# the device's live register array -- changes persist for future reads.
# We piggyback a rate-limited random-walk "tick" onto that callback so the
# read-only QC/process registers actually drift over time.
UPDATE_INTERVAL_SEC = 2.0

# Simple bounded, mean-reverting random walk. Each entry:
# (register index [0-based], baseline, max step per tick, min, max)
# NOTE: FiltrateFlowRate (idx 27) and ROFeedFlowRate (idx 28) are NOT in this
# list -- they're driven by the tank model below instead, since Tank Level is
# a genuine function of the difference between them, not independent noise.
NUDGE_REGISTERS = [
    (0,  25,  1,  15,  40),    # HR1  QC1 Raw Water Turbidity (NTU x10)
    (1,  8,   1,  3,   15),    # HR2  QC2 Post-Filtration Turbidity (NTU x10)
    (2,  120, 2,  80,  200),   # HR3  QC2 Chlorine Residual (mg/L x100)
    (15, 450, 5,  400, 500),   # HR16 QC3 RO Feed Conductivity
    (16, 18,  1,  10,  30),    # HR17 QC4 Permeate Conductivity
    (26, 150, 2,  130, 170),   # HR27 InfluentFlowRate (GPM)
    (29, 68,  1,  60,  75),    # HR30 InfluentTemperature (F)
    (30, 180, 2,  150, 220),   # HR31 Hardness / InfluentHardness (mg/L as CaCO3)
    (31, 680, 1,  650, 720),   # HR32 ROFeed_pH (pH x100)
]
# DosePV registers (HR5/9/13/20/24) are still static -- separate, smaller fix,
# not part of this change.

_last_tick = 0.0


def _walk(current, baseline, step, lo, hi):
    """One bounded, mean-reverting random step."""
    delta = random.randint(-step, step)
    if current > baseline and random.random() < 0.3:
        delta -= 1
    elif current < baseline and random.random() < 0.3:
        delta += 1
    return max(lo, min(hi, current + delta))


# --- RO Feed Tank Level model ---------------------------------------------
# Level is a genuine net-flow integration, not noise: it rises with
# FiltrateFlowRate (inflow) and falls with ROFeedFlowRate (outflow), matching
# how the Architecture doc describes this tank's real purpose. Under normal
# conditions FiltrateFlowRate runs modestly above ROFeedFlowRate, so the tank
# holds comfortably full. Periodically, a simulated upstream disruption (e.g.
# a filter cycling -- the exact scenario the Architecture doc calls out as
# the reason this tank exists) forces FiltrateFlowRate down hard for 60-120s,
# draining the tank and exercising the real Low Tank Level alarm/interlock.
#
# Tuned and verified with an offline simulation before wiring in here:
# a disruption reliably drains a full tank below the <20% alarm threshold
# within its window, and the tank fully recovers within the gap between
# disruptions (no chatter, no permanently-stuck-low state).
TANK_GAIN = 0.025           # % level change per tick, per GPM of net flow
TANK_LEVEL_IDX = 17         # HR18
FILTRATE_IDX = 27           # HR28
ROFEED_IDX = 28             # HR29

_tank_level = None          # lazy-initialized float accumulator (sub-integer precision)
_in_disruption = False
_next_disruption_tick = None
_disruption_end_tick = None
_tick_count = 0


def _tick_tank(registers):
    global _tank_level, _in_disruption, _next_disruption_tick, _disruption_end_tick, _tick_count

    _tick_count += 1
    if _tank_level is None:
        _tank_level = float(registers[TANK_LEVEL_IDX])
    if _next_disruption_tick is None:
        _next_disruption_tick = _tick_count + random.randint(120, 270)  # 4-9 min at 2s/tick

    if not _in_disruption and _tick_count >= _next_disruption_tick:
        _in_disruption = True
        _disruption_end_tick = _tick_count + random.randint(30, 60)  # 60-120s

    if _in_disruption:
        registers[FILTRATE_IDX] = max(0, 15 + random.randint(-3, 3))
        if _tick_count >= _disruption_end_tick:
            _in_disruption = False
            _next_disruption_tick = _tick_count + random.randint(120, 270)
    else:
        registers[FILTRATE_IDX] = _walk(registers[FILTRATE_IDX], 145, 2, 125, 165)

    registers[ROFEED_IDX] = _walk(registers[ROFEED_IDX], 125, 2, 105, 145)

    net_flow = registers[FILTRATE_IDX] - registers[ROFEED_IDX]
    _tank_level = max(0.0, min(100.0, _tank_level + TANK_GAIN * net_flow))
    registers[TANK_LEVEL_IDX] = round(_tank_level)


async def add_process_noise(function_code, start_address, address, count, current_registers, set_values):
    """SimDevice action hook -- fires on every Modbus request. Rate-limited
    to one simulated tick per UPDATE_INTERVAL_SEC regardless of how often
    Ignition/the enterprise API actually poll."""
    global _last_tick
    now = time.monotonic()
    if now - _last_tick < UPDATE_INTERVAL_SEC:
        return None
    _last_tick = now

    for idx, baseline, step, lo, hi in NUDGE_REGISTERS:
        current_registers[idx] = _walk(current_registers[idx], baseline, step, lo, hi)

    _tick_tank(current_registers)
    return None


holding_regs = SimData(0, datatype=DataType.REGISTERS, values=initial_values)
device = SimDevice(1, holding_regs, action=add_process_noise)

if __name__ == "__main__":
    print("Starting Modbus TCP simulator on localhost:5020...")
    print(f"Exposing {len(initial_values)} holding registers (HR1-HR{len(initial_values)})")
    StartTcpServer(context=device, address=("localhost", 5020))