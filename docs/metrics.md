# Métricas — payment-core-api

Regla: todo número enlaza a su recibo reproducible. (Spec §3: "recibos, no promesas").

## Recibos

- [ ] CI verde en el último commit: [`https://github.com/<usuario>/payment-core-api/actions`](https://github.com/jjesusdev/payment-core-api/actions)
  - Run: test + build-image en verde
- [ ] Tests unitarios (Mockito): 15 verdes (`mvn test`)
- [ ] Tests de integración (Testcontainers PostgreSQL 16): 12 verdes (`mvn verify` + TransferFlowIT)
- [ ] Test de concurrencia (locking): 8 transferencias paralelas → saldos exactos
- [ ] demos local: `docker compose up` + curl del README funcionando
- [ ] Flyway: V1→V3 aplicadas desde cero en contenedor real

## Cobertura

- [ ] `mvn jacoco:report` (agregar plugin en semana 2 o aquí si se desea) → % global

## Rendimiento

- [ ] p50 de `POST /api/transfers` local (marcar fecha y comando: `curl -w`/`time`)