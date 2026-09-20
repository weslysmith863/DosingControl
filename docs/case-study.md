# Adaptive Dosing Control Platform: Case Study

## Overview

The Adaptive Dosing Control Platform is a working simulation of an ISA-95 enterprise-to-control integration for industrial water treatment, built end to end: a Java/Spring Boot enterprise layer, an Ignition Perspective SCADA layer, and a Python process simulator talking real industrial protocol underneath both. I built it to demonstrate something specific: that I understand how automation and enterprise software actually connect in a working plant, not just how to configure tags in a training environment.

The process itself is not generic. Before starting this project, I worked hands-on installing industrial water treatment equipment (coagulant and sodium hypochlorite dosing skids, media filtration, RO trains) at industrial sites in Southeast Texas for Evoqua and Xylem. Every tag, alarm, and control loop in this project maps to something I watched a real system do in the field, not something copied from a tutorial.

## Architecture: Three Layers

The system is built as three layers, matching the ISA-95 model that system integrators are actually hired to build and maintain:

| Layer | ISA-95 Level | Technology | Responsibility |
|---|---|---|---|
| Enterprise | Level 4/3 | Java, Spring Boot, MySQL | Dosing formula definitions, versioning and approval, water quality and compliance records, alarm history |
| Control / SCADA | Level 2 | Ignition (Gateway, Perspective, Tags, Alarming) | Automatic dosing control, live process monitoring, live alarm and error log, historization |
| Process / Field | Level 0/1 (simulated) | Python, Modbus TCP | Simulated pretreatment, media filtration, and RO train that responds to dosing the way real equipment does |

I deliberately did not use Ignition's built-in tag simulator. It generates values internally with no real protocol and no way to react to a setpoint Ignition writes back, which meant it could not support the closed-loop dosing behavior the project depends on. Instead I wrote a custom Python simulator using pymodbus, exposed over Modbus TCP on port 5020, with Ignition connecting through its native Modbus driver. That is the same integration skill an SI does on a real job: polling a device, mapping registers to tags, handling comms faults. It is a simulation, but it is not a toy.

## The Process

The treatment train follows the sequence I saw run in the field: raw water intake, then a QC1 check (raw water turbidity and hardness), then two automatic dosing steps in parallel, coagulant to clump sediment and sodium hypochlorite to control organic and biological fouling. Water then passes through media filtration, followed by a QC2 check that specifically measures the chlorine and ORP residual left behind by the SH dose. That residual drives a third dosing step, neutralizer, before the water reaches an RO feed holding tank and then the RO membrane system itself, finishing with a QC3 permeate check before the water goes to plant use.

Two details in that sequence exist for real, high-stakes reasons, not because a tutorial called for them:

The neutralizer step exists because undosed or under-dosed neutralizer lets residual chlorine reach the RO membranes and oxidatively damage them, an expensive failure mode. That is a credible, field-grounded alarm condition, not a generic threshold picked to have something to alarm on.

The RO feed holding tank exists because without it, small upstream flow fluctuations (a filter cycling, a dosing pump ramping) can trip the RO on low inlet flow, a nuisance shutdown that stresses membranes and costs production time if it happens repeatedly. The tank buffers those fluctuations so the RO always sees a stable feed, exactly how I saw it handled on site.

## The Control Logic

Two mechanisms run continuously off live tag values. There is no operator launch step, because the real system does not have one either:

**The dosing calculation loop.** One generic Gateway script runs for all five chemical types, coagulant, sodium hypochlorite, neutralizer, antiscalant, and caustic, not five separate hardcoded loops. For each one, it calls the enterprise API for that chemical's currently active formula, reads the live QC and process tags that formula depends on, calculates the dose, clamps it to the formula's min/max, and writes the pump's setpoint. The same mechanism runs identically whether the formula behind it is carefully tuned or a placeholder, see Judgment Calls below for which is which.

**RO feed stabilization.** The RO feed pump draws from the holding tank instead of directly off the filtration and dosing stage, so short upstream flow dips never reach the RO. A low tank level interlock holds or alarms the feed pump if the tank runs low, rather than letting the RO itself trip on low flow.

## The Adaptive Part

Nothing in this system has an operator picking a recipe. What actually governs the dosing calculation loop is a set of versioned dosing formulas, one per chemical, for example Coagulant Curve v2, SH Dose Curve v1, and Neutralizer Curve v3, that get defined and approved in the enterprise layer. Ignition always executes whichever version is currently active. The intent is that engineers tune these formulas over time based on the water quality and compliance data the system is collecting, then approve a new active version, so better data in produces better tuned formulas out and measurably better compliance numbers over time. That tuning loop is where the platform's name comes from.

The data moves both directions. Down, an engineer tunes and approves a formula version in the Spring Boot app, Ignition pulls the active formula, and its control loop scripts continuously evaluate it against live QC readings to calculate real time setpoints. Ignition continuously logs QC1, QC2, and QC3 readings, actual dosing rates, and which formula version was active, back to MySQL as the compliance record, while alarm events (chlorine breakthrough risk, pump fault, RO differential pressure high, tank level low) log through Ignition's built-in Alarm Journal to the same database and surface live on the Perspective HMI's alarm log.

## Judgment Calls

A few decisions in this project were deliberate, and I think they are worth explaining directly rather than leaving them implicit.

**I describe this as continuous process control, not a batch process.** It would be easy to reach for words like recipe, batch, or run, since that vocabulary is common in a lot of automation tutorials. But this plant does not run batches. Water flows through continuously, and what changes over time is which formula version is active, not which batch is running. Using ISA-88 batch language here would be a real mistake, not just a style choice, since it signals a gap in process control fundamentals to anyone in SCADA who actually knows the difference. I made a point of keeping the vocabulary accurate throughout the project and this write-up.

**I am calling the enterprise layer a model of the ISA-95 boundary, not a full MES.** It would be tempting to oversell the Spring Boot layer as a manufacturing execution system, since that sounds more impressive. It is not one. It does not do scheduling, genealogy, or multi-site coordination. What it actually does, cleanly, is formula versioning and approval plus the quality and alarm record, which is a real and specific slice of what an MES does at the enterprise-to-control boundary. I would rather a technical interviewer confirm I know exactly what I built than catch me overselling it.

**I invested real time making the simulator argue for itself.** A dosing calculation that is just a straight line from quality reading to setpoint reads as a toy to anyone who has done real control work. So the simulator includes flow pacing rather than quality-only dosing, dead time between a setpoint change and the process responding, and sensor noise. None of that was strictly necessary to make the demo run. It was necessary to make the control loop math hold up under a real technical follow-up question.

**I built one dosing engine for all five chemicals, and I only tuned one of them.** The dosing calculation loop is intentionally chemical-agnostic, it runs identically for coagulant, SH, neutralizer, antiscalant, and caustic, driven entirely by whichever formula is active for that chemical. Coagulant is the one formula I actually reasoned through against realistic process behavior. The other four run through that same live mechanism with placeholder coefficients, which proves the engine generalizes, it does not stand in for tuned production values. I would rather be specific about which is which than let five running pumps imply five finished formulas.

## Tech Stack

| Component | Tool |
|---|---|
| Backend API | Java 25, Spring Boot 4.1.x |
| Database | MySQL 8 |
| SCADA / HMI | Ignition (Maker Edition), Perspective module |
| Process simulation | Python 3.14, pymodbus, Modbus TCP |
| Integration protocol | Modbus TCP (device layer), REST/JSON (enterprise to control) |
| Alarm and event storage | Ignition Alarm Journal, logged to MySQL |
| Version control | Git / GitHub |

## Where This Stands

The full stack runs end to end today: MySQL restored with all compliance and formula data intact, the Python simulator driving live process values over Modbus TCP, the Ignition Gateway and Designer connected and reading those values, and the Spring Boot API serving real dosing formula data to Ignition. The remaining work is presentation, not function: a recorded demo walkthrough of the finished interface.
