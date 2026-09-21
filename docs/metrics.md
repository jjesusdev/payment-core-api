# Métricas — payment-core-api

Regla: todo número enlaza a su recibo reproducible. (Spec §3: "recibos, no promesas").

## Recibos

- [x] CI verde en el último commit: [`https://github.com/jjesusdev/payment-core-api/actions`](https://github.com/jjesusdev/payment-core-api/actions)
  - Run: test + build-image en verde
- [x] Tests unitarios (Mockito): 13 verdes (`mvn test`) — AuthServiceTest 5, TransactionServiceTest 6, AccountServiceTest 2
- [x] Tests de integración (Testcontainers PostgreSQL 16): 12 verdes (`mvn verify`) — AuthFlowIT 4, TransferFlowIT 4, AccountFlowIT 3, FlywayIT 1
  - Recibo: `mvn -B verify` → `MVN EXIT: 0`, `Tests run: 12, Failures: 0, Errors: 0` (2026-09-21)
- [x] Test de concurrencia (locking): 8 transferencias paralelas → saldos exactos
  - Recibo: `TransferFlowIT.eightConcurrentTransfersPreserveBalanceWithLocking` verde contra Postgres real
- [x] demos local: `docker compose up` + curl del README funcionando
  - Recibo (2026-09-21): register jose/ana → cuentas `1147483326`/`1595686148`, depósito 500→`BAL_A=500.00`, transferencia 100→`BAL_A=400.00`/`BAL_B=100.00`, sin token `401`, acceso a cuenta ajena `403 ACCOUNT_ACCESS_DENIED`
- [x] Flyway: V1→V3 aplicadas desde cero en contenedor real
  - Recibo: `GET /actuator/health` → `db: UP` (PostgreSQL) tras arranque limpio del compose

## Notas de ingeniería (2026-09-21)

- Contenedor de Testcontainers: `BaseIntegrationTest` usa `static { POSTGRES.start(); }` (una vez por JVM) en lugar de `@Testcontainers`/`@Container`, porque el ciclo por clase detenía el contenedor heredado y el contexto Spring cacheado quedaba apuntando a un puerto muerto → `Failed to validate connection ... This connection has been closed`.
- Transferencia: se bloquean ambas cuentas con `SELECT ... FOR UPDATE` en orden de id ascendente **antes** de cualquier lectura de la entidad. En una versión previa la lectura inicial (sin lock) dejaba la entidad en el persistence context, el `SELECT ... FOR UPDATE` no la refrescaba y el flush fallaba con `StaleObjectStateException` bajo concurrencia.
- `GlobalExceptionHandler`: las excepciones no controladas se loguean (`log.error`) para no perder la causa raíz tras el `500`.

## Cobertura

- [ ] `mvn jacoco:report` (agregar plugin en semana 2 o aquí si se desea) → % global

## Rendimiento

- [ ] p50 de `POST /api/transfers` local (marcar fecha y comando: `curl -w`/`time`)