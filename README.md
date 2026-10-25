# payment-core-api

Core transaccional de pagos: cuentas, depósitos, transferencias, saldos e historial.
Backend Java: Spring Boot 3, Java 21, PostgreSQL 16 + Flyway, Spring Security + JWT.

La decisión clave del dominio: una transferencia es una sola transacción (`@Transactional`)
que bloquea ambas cuentas con `SELECT ... FOR UPDATE` en orden de id ascendente, garantizando
débito y crédito atómicos sin deadlocks (8 transferencias concurrentes = saldos exactos).

## Arquitectura

```
client ──► /api/auth (JWT) ──► /api/accounts, /api/transfers (seguro)
                                   │
                                   ▼
                    PostgreSQL 16 (Flyway: schema de los 3 worlds)
```

## Quick start (docker-compose)

```bash
docker compose up -d --build

# 1. Registro → token
TOKEN=$(curl -s -X POST localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"jose","password":"secreto123"}' | jq -r .token)

# 2. Crear cuenta
ACCTA=$(curl -s -X POST localhost:8080/api/accounts \
  -H "Authorization: Bearer $TOKEN" | jq -r .accountNo)

# 3. Depósito y saldo
curl -s -X POST localhost:8080/api/accounts/$ACCTA/deposits \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"amount":500.00}'
curl -s localhost:8080/api/accounts/$ACCTA/balance -H "Authorization: Bearer $TOKEN"

# 4. Transferencia a otra cuenta
ACCTB=$(curl -s -X POST localhost:8080/api/accounts \
  -H "Authorization: Bearer $(curl -s -X POST localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' -d '{"username":"ana","password":"secreto123"}' | jq -r .token)" | jq -r .accountNo)
curl -s -X POST localhost:8080/api/transfers \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d "{\"fromAccountNo\":\"$ACCTA\",\"toAccountNo\":\"$ACCTB\",\"amount\":100.00}"
```

## Endpoints

| Método | Ruta | Auth | Descripción |
|---|---|---|---|
| POST | /api/auth/register | No | Crea usuario, devuelve JWT |
| POST | /api/auth/login | No | Inicia sesión, devuelve JWT |
| POST | /api/accounts | JWT | Crea cuenta (nº único, saldo 0) |
| GET | /api/accounts/{no} | JWT | Detalle de cuenta (solo owner) |
| GET | /api/accounts/{no}/balance | JWT | Saldo |
| POST | /api/accounts/{no}/deposits | JWT | Depósito (`{amount}`) |
| POST | /api/transfers | JWT | Transferencia (`{fromAccountNo,toAccountNo,amount}`) |
| GET | /actuator/health | No | Healthcheck (K8s probes) |

## Estado de errores

`422` monto inválido / misma cuenta · `404` cuenta inexistente · `409` saldo insuficiente /
cuenta inactiva · `403` sin acceso a la cuenta · `401` sin/mala autenticación.

## Tests

```bash
mvn test          # unitarios (Mockito)
mvn verify        # + integración (Testcontainers PostgreSQL 16)
```

Ver [docs/metrics.md](docs/metrics.md) para los recibos (recuentos reales de este repo).
## OpenAPI / Swagger

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Idempotencia

Las transferencias admiten la cabecera `Idempotency-Key` (UUID). Si se repite la misma clave con el mismo cuerpo, se devuelve la respuesta almacenada. Si se reutiliza con un cuerpo distinto, se responde `409 IDEMPOTENCY_CONFLICT`.

## Paquete de imagen

En `main`, el workflow publica `ghcr.io/jjesusdev/payment-core-api:latest` y `ghcr.io/jjesusdev/payment-core-api:<sha>`.
