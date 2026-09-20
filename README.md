# Adaptive Dosing Control Platform

A working ISA-95 enterprise-to-control integration for industrial water treatment: a Java/Spring Boot backend for dosing-formula governance and compliance records, integrated bi-directionally with an Ignition SCADA control layer, driven by a custom Python/Modbus process simulator instead of a canned tag simulator.

Built by a former field technician who installed this exact class of equipment (coagulant/SH dosing skids, media filtration, RO trains) for Evoqua and Xylem before writing the software for it. Full writeup: [docs/case-study.md](docs/case-study.md).

## Architecture

| Layer | ISA-95 Level | Technology | Responsibility |
|---|---|---|---|
| Enterprise | Level 4/3 | Java, Spring Boot, MySQL | Dosing formula definitions, versioning and approval, water quality and compliance records, alarm history |
| Control / SCADA | Level 2 | Ignition (Gateway, Perspective, Tags, Alarming) | Automatic dosing control, live process monitoring, live alarm and error log, historization |
| Process / Field (simulated) | Level 0/1 | Python, Modbus TCP | Simulated pretreatment, media filtration, and RO train that responds to dosing the way real equipment does |

This repository is the enterprise layer. It owns one job: govern which dosing formula is currently authorized for each chemical, and record what actually happened. It does not run the control loop, that's Ignition's job, reading the active formula from this API and executing against live tags.

## Formula lifecycle

Every dosing formula moves through a fixed state machine, enforced in `DosingFormulaService`:

```
DRAFT --approve--> APPROVED --activate--> ACTIVE --(next version activated)--> RETIRED
```

Activating a new formula for a chemical automatically retires whatever was previously active for that same chemical, inside one transaction, so there is never more than one active formula per chemical at a time and every past version stays in the table as a permanent audit record.

**Scope note:** the schema supports five chemical types (`COAGULANT`, `SH`, `SBS`, `ANTISCALANT`, `CAUSTIC`) so the calculation loop generalizes across every dosing point in the process. Only `COAGULANT` currently has a formula tuned against realistic process behavior; the other four are seeded with arbitrary placeholder coefficients (see `requests.http`) to exercise the full five-pump calculation path end to end, not to represent tuned production values.

## API

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/api/formulas` | List every formula, any status |
| `GET` | `/api/formulas/{id}` | Get one formula by id |
| `GET` | `/api/formulas/active/{chemicalType}` | The endpoint Ignition polls for the live formula for a given chemical |
| `POST` | `/api/formulas` | Create a new DRAFT (auto-versioned per chemical type) |
| `PUT` | `/api/formulas/{id}/approve` | DRAFT → APPROVED |
| `PUT` | `/api/formulas/{id}/activate` | APPROVED → ACTIVE (retires the previous active formula for that chemical) |
| `GET` / `POST` | `/api/readings` | QC/process values pushed from Ignition |
| `GET` / `POST` | `/api/alarms` | Alarm events pushed from Ignition's Alarm Pipeline, the persisted compliance/alarm log |

Full example requests, including creating and activating all five chemical types, are in [requests.http](requests.http).

## Running it locally

**Requires:** Java 25, MySQL 8, Maven (wrapper included).

1. Create the database and set credentials as environment variables (never committed):
   ```
   DB_USERNAME=<your MySQL user>
   DB_PASSWORD=<your MySQL password>
   ```
   The app expects a database named `adaptive_dosing_control` at `localhost:3306` (see `application.properties`).

2. Run the API:
   ```
   ./mvnw spring-boot:run
   ```
   Starts on `localhost:8080`.

3. (Optional, for the full simulated process) Run the Python Modbus simulator and connect an Ignition Gateway to it over Modbus TCP on port 5020, and to this API's endpoints above for formula data and alarm/reading write-back.

## Tech stack

| Component | Tool |
|---|---|
| Backend API | Java 25, Spring Boot 4.1.x |
| Database | MySQL 8 |
| SCADA / HMI | Ignition (Maker Edition), Perspective module |
| Process simulation | Python 3.14, pymodbus, Modbus TCP |
| Integration protocol | Modbus TCP (device layer), REST/JSON (enterprise to control) |
| Version control | Git / GitHub |
