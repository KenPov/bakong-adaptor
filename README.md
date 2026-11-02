# bakong-adaptor

Spring Boot 3.4.x (Java 25 LTS / Java 21+) re-implementation of two flows from the WSO2 ESB/Synapse project
`open_banking_integration/Bakong-Open-APIs`, as the first step of migrating SBI LY HOUR Bank's NBC/Bakong
open-banking integration off WSO2 ESB.

The app itself has **no servlet context path** — client-facing URLs are unchanged from the legacy WSO2
contract. When deployed behind the Traefik gateway (`docker-compose.uat.yml` / `docker-compose.prod.yml`, in the parent directory),
routing to this service is done by matching these exact endpoint paths, not a shared prefix, so clients
never need to add anything like `/bakong-adaptor` to their calls.

## Scope

| Endpoint | Legacy source | Purpose |
|---|---|---|
| `GET /api/v1/account-inquiry-to-other-bank` | `account-inquiry-to-other-bank.xml` | Our mobile app verifies another bank's account via the Bakong OB hub before a transfer. |
| `POST /api/v1/bakong-full-fund-transfer` | `bakong-full-fund-transfer.xml` | Single-shot interbank transfer, built as an ISO 20022 pain.001 message sent over SOAP to the CIFTP core-banking adapter. |

Out of scope (still live in the WSO2 ESB project, not touched here): the own-bank `account-inquiry` API,
and the two-step OTP wallet-transfer flow (`init-transaction`/`finish-transaction`).

**Both endpoints always return HTTP 200** — success/failure is signalled purely via `status.code` (`0` =
success, `1` = error) in the response body, matching the legacy Synapse contract that existing consumers
already rely on.

## Known legacy quirks carried forward or resolved

- `account_data.bank_code` prefers `accountInfo.localPartCode` (what the legacy mapper actually reads,
  even though the documented upstream schema only defines `bankCode`), falling back to `bankCode` if
  absent. See `AccountInquiryResponseMapper`.
- `paymentEndToEndId`/`paymentInstrId` are accepted as required request fields for full-fund-transfer but,
  matching legacy, are **not** used when building the outbound ISO 20022 message — `EndToEndId`/`InstrId`
  are derived independently. See `FullFundTransferRequest`.
- The `"conrner"` typo in the bank's postal address has been fixed to `"corner"` in `application.yml`
  (`our-bank.postal-address.street-name`) — free-text metadata, not a reconciliation key.
- The fragile `substring`/`indexOf` scraping the legacy Synapse JS used to pull `OrgnlMsgId`/`Ustrd`/
  `OrgnlPmtInfId` out of the SOAP response has been replaced with proper DOM parsing. See
  `SoapResponseParser`.

## Configuration

All downstream URLs and credentials have default values configured in `application.yml` for local development / SIT testing. You can run the application directly without needing any `.env` file.

To override any property for higher environments (UAT/Production) or local custom testing, export environment variables:

| Variable | Default Value | Purpose |
|---|---|---|
| `SERVER_PORT` | `8082` | Service HTTP port |
| `BAKONG_OB_CLIENT_AUTH_URL` | `http://172.24.21.20:8080/v1/authenticate` | Bakong OB hub login endpoint |
| `BAKONG_OB_CLIENT_ACCOUNT_INQUIRY_URL` | `http://172.24.21.20:8080/api/v1/account_inquiry/account` | Bakong OB hub account-inquiry endpoint |
| `BAKONG_OB_CLIENT_USERNAME` | `sbilh_app_sit` | Bakong OB hub username |
| `BAKONG_OB_CLIENT_PASSWORD` | `vw7w2bwqXEzLmAZ9uPN0` | Bakong OB hub password |
| `CIFTP_API_URL` | `http://spguat.sbilhbank.com.kh:8765` | Base URL of CIFTP core-banking SOAP adapter |
| `CIFTP_API_USER` | `spgapp` | CIFTP `makeFullFundTransfer` username |
| `CIFTP_API_PWD` | `Spg@ppPa440rd23` | CIFTP `makeFullFundTransfer` password |

## Build & test

```shell
mvn clean test
```

## Run locally

```shell
mvn spring-boot:run
```

## Run with Docker

```shell
# Build and run locally with Docker Compose (uses default configurations)
docker compose up -d --build
```

This builds the image via the multi-stage `Dockerfile` (Maven build stage → minimal
`eclipse-temurin:25-jre-jammy` runtime stage running as a non-root user) and starts the service on
`http://localhost:8080` (override the host port with `APP_PORT` in `.env`). `docker-compose.yml` reads
`.env` for the same downstream URLs/credentials described above, and a container `HEALTHCHECK`/Compose
healthcheck poll `/actuator/health/liveness` (Spring Boot Actuator is included for this).

The Bakong OB hub and CIFTP adapter are reached at internal bank network addresses (e.g. the OB hub is
currently at `172.24.21.168` in SIT) — the Docker host running this container needs routable network access
to those addresses; no extra service is defined in `docker-compose.yml` for them since they're external
systems, not something this project runs.

See `RUNBOOK.md` for manual SIT smoke-test curl commands.
