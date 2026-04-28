# API Mobile - Backlog Resuelto

Este archivo queda como referencia historica del backlog que se pidio para cerrar la app movil de pasajeros.

## Implementado el 2026-04-28

- `POST /api/v1/auth/register`
- `GET /api/v1/buses/by-code/{code}`
- resumen enriquecido de ruta dentro de `GET /api/v1/buses`
- respuestas enriquecidas de pagos con `userId`, `busId`, `busPlate`, `routeName`, `routeOrigin`, `routeDestination`
- `GET /api/v1/wallet`
- `POST /api/v1/wallet/top-ups`
- `GET /api/v1/wallet/transactions`
- debito real de billetera al pagar con `method = WALLET`
- re-acreditacion de billetera al revertir un pago `WALLET`

## Fuente actual

La documentacion vigente para consumo movil esta en [ENDPOINTS_APP_MOVIL.md](/Users/diego/IdeaProjects/api-buses/ENDPOINTS_APP_MOVIL.md).
