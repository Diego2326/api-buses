# API Buses - Endpoints para App Movil

- Base URL: `https://<host-del-backend>/api/v1`
- Auth header: `Authorization: Bearer <jwt>`
- Content-Type: `application/json`
- Fecha de referencia: `2026-04-28`

## Auth

### `POST /auth/login`

Body:

```json
{
  "email": "admin@buses.gt",
  "password": "tu-password"
}
```

Response:

```json
{
  "token": "jwt",
  "user": {
    "id": "uuid",
    "name": "Administrador",
    "email": "admin@buses.gt",
    "role": "ADMIN",
    "status": "ACTIVE"
  }
}
```

### `POST /auth/register`

Registro publico. Crea usuario `PASSENGER` en estado `ACTIVE` y devuelve JWT.

Body:

```json
{
  "name": "Ana Morales",
  "email": "ana@buses.gt",
  "password": "123456"
}
```

Response:

```json
{
  "token": "jwt",
  "user": {
    "id": "uuid",
    "name": "Ana Morales",
    "email": "ana@buses.gt",
    "role": "PASSENGER",
    "status": "ACTIVE"
  }
}
```

### `GET /auth/me`

Response:

```json
{
  "id": "uuid",
  "name": "Ana Morales",
  "email": "ana@buses.gt",
  "role": "PASSENGER",
  "status": "ACTIVE"
}
```

### `POST /auth/logout`

Response:

```json
{
  "success": true
}
```

## Permisos

- `GET /api/v1/**`: `ADMIN`, `OPERATOR`, `INSPECTOR`, `PASSENGER`
- `POST /api/v1/payments`: `ADMIN`, `OPERATOR`, `PASSENGER`
- `POST /api/v1/wallet/top-ups`: `ADMIN`, `OPERATOR`, `PASSENGER`
- Otros `POST/PUT/PATCH` en `/api/v1/**`: `ADMIN`, `OPERATOR`
- `POST /api/v1/auth/login` y `POST /api/v1/auth/register`: publicos

## Convenciones

### Paginacion

Query params comunes:

- `page`: base 0
- `size`: default `20`
- `sort`: ejemplo `sort=name,asc`

Formato:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

### Errores

```json
{
  "timestamp": "2026-04-28T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "La solicitud contiene errores de validacion.",
  "path": "/api/v1/auth/register",
  "details": [
    {
      "field": "email",
      "message": "must be a well-formed email address"
    }
  ]
}
```

### Enums

- `OperationalStatus`: `ACTIVE`, `INACTIVE`, `MAINTENANCE`, `SUSPENDED`
- `PaymentStatus`: `COMPLETED`, `PENDING`, `FAILED`, `REVERSED`
- `PaymentMethod`: `CARD`, `QR`, `CASH`, `WALLET`
- `UserRole`: `ADMIN`, `OPERATOR`, `INSPECTOR`, `PASSENGER`
- `WalletTransactionType`: `TOP_UP`, `PAYMENT`, `REVERSAL`
- `WalletTransactionStatus`: `COMPLETED`, `FAILED`

### Coordenadas

La API mezcla formatos:

- `stops.position`, `routes.stops[].position`, `dashboard.mapMarkers`, `operations-map.*Markers`, `operations-map.routePaths[].points`: `[latitude, longitude]`
- `routes.geometry.coordinates`: `[longitude, latitude]`

## Buses

### `GET /buses`

Filtros:

- `search`: busca por `plate` o `code`
- `status`
- `routeId`
- `page`, `size`, `sort`

El resumen de ruta ya viene enriquecido:

```json
{
  "id": "uuid",
  "plate": "C 102 BAA",
  "code": "BUS-102",
  "capacity": 55,
  "route": {
    "id": "uuid",
    "name": "Ruta 12 Centro",
    "origin": "Terminal Oriente",
    "destination": "Parque Central"
  },
  "status": "ACTIVE"
}
```

### `GET /buses/by-code/{code}`

Lookup directo por codigo exacto.

### `GET /buses/{id}`

Devuelve `BusResponse`.

### `POST /buses`

```json
{
  "plate": "C 150 BAA",
  "code": "BUS-150",
  "capacity": 50,
  "routeId": "uuid o null",
  "status": "ACTIVE"
}
```

Reglas:

- `capacity >= 1`
- no se puede asignar a una ruta `SUSPENDED`

### `PUT /buses/{id}`

Mismo body que crear.

### `PATCH /buses/{id}/status`

```json
{
  "status": "MAINTENANCE"
}
```

### `PATCH /buses/{id}/route`

```json
{
  "routeId": "uuid o null"
}
```

## Paradas

### `GET /stops`

Filtros: `search`, `status`, `page`, `size`, `sort`

### `GET /stops/{id}`

### `POST /stops`

```json
{
  "code": "P-009",
  "name": "Nueva Parada",
  "address": "Zona 1",
  "latitude": 14.970000,
  "longitude": -89.530000,
  "status": "ACTIVE"
}
```

### `PUT /stops/{id}`

### `PATCH /stops/{id}/status`

## Rutas

### `GET /routes`

Filtros: `search`, `status`, `page`, `size`, `sort`

En listado, `geometry` y `stops[].position` vienen `null`.

### `GET /routes/{id}`

Devuelve geometria y posiciones completas.

### `POST /routes`

```json
{
  "name": "Ruta Centro Norte",
  "stopIds": ["uuid-1", "uuid-2", "uuid-3"],
  "status": "ACTIVE"
}
```

Reglas:

- minimo 2 paradas
- no repetir parada
- `origin` = primera parada
- `destination` = ultima parada

### `PUT /routes/{id}`

### `PATCH /routes/{id}/status`

### `POST /routes/{id}/recalculate-geometry`

## Tarifas

### `GET /fares`

Filtros: `search`, `status`, `page`, `size`, `sort`

### `GET /fares/{id}`

### `POST /fares`

```json
{
  "name": "Tarifa urbana",
  "amount": 4.00,
  "validFrom": "2026-01-01",
  "validTo": "2026-12-31",
  "status": "ACTIVE"
}
```

### `PUT /fares/{id}`

### `PATCH /fares/{id}/status`

## Pagos

### `GET /payments`

Filtros:

- `userId`
- `busId`
- `status`
- `method`
- `dateFrom`
- `dateTo`
- `page`, `size`, `sort`

### `GET /payments/{id}`

### `POST /payments`

```json
{
  "userId": "uuid",
  "busId": "40000000-0000-0000-0000-000000000102",
  "amount": 4.0,
  "method": "WALLET",
  "externalReference": "mobile-wallet-001"
}
```

Reglas:

- `amount >= 0.01`
- si el actor autenticado es `PASSENGER`, solo puede pagar para su propio `userId`
- si `method = WALLET`, el usuario pagado debe ser `PASSENGER`
- si `method = WALLET`, valida saldo disponible

Respuesta enriquecida:

```json
{
  "id": "uuid",
  "userId": "uuid",
  "user": "Ana Morales",
  "busId": "uuid",
  "bus": "BUS-102",
  "busPlate": "C 102 BAA",
  "routeName": "Ruta 12 Centro",
  "routeOrigin": "Terminal Oriente",
  "routeDestination": "Parque Central",
  "amount": 4.0,
  "date": "2026-04-28T14:15:00Z",
  "status": "COMPLETED",
  "method": "WALLET"
}
```

### `POST /payments/{id}/reverse`

```json
{
  "reason": "Cobro duplicado"
}
```

Si el pago original fue `WALLET`, la reversa re-acredita saldo en billetera.

## Wallet

Disponible para usuarios `PASSENGER`.

### `GET /wallet`

```json
{
  "balance": 42.5,
  "currency": "GTQ"
}
```

### `POST /wallet/top-ups`

```json
{
  "amount": 20.0,
  "method": "CARD"
}
```

Reglas:

- `amount >= 0.01`
- `method` no puede ser `WALLET`

Response:

```json
{
  "id": "uuid",
  "amount": 20.0,
  "status": "COMPLETED",
  "date": "2026-04-28T15:00:00Z"
}
```

### `GET /wallet/transactions`

Paginado, ordenado por fecha descendente.

```json
{
  "content": [
    {
      "id": "uuid",
      "type": "PAYMENT",
      "amount": 4.0,
      "date": "2026-04-28T15:10:00Z",
      "status": "COMPLETED"
    },
    {
      "id": "uuid",
      "type": "TOP_UP",
      "amount": 20.0,
      "date": "2026-04-28T15:00:00Z",
      "status": "COMPLETED"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 2,
  "totalPages": 1
}
```

## Usuarios

### `GET /users`

Filtros: `search`, `role`, `status`, `page`, `size`, `sort`

### `GET /users/{id}`

### `POST /users`

```json
{
  "name": "Ana Morales",
  "email": "ana@buses.gt",
  "password": "123456",
  "role": "PASSENGER",
  "status": "ACTIVE"
}
```

### `PUT /users/{id}`

### `PATCH /users/{id}/status`

### `PATCH /users/{id}/role`

### `POST /users/{id}/reset-password`

## Dashboard y reportes

- `GET /dashboard`
- `GET /operations-map`
- `GET /reports/summary`
- `GET /reports/payments`
- `GET /reports/routes`
- `GET /reports/buses`
- `POST /reports/schedules`

`reports/payments` reutiliza la misma respuesta enriquecida de `PaymentResponse`.
