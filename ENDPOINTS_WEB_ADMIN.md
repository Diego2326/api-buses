# API Buses - Endpoints para Web Admin

- Base URL: `https://<host-del-backend>/api/v1`
- Auth header: `Authorization: Bearer <jwt>`
- Content-Type: `application/json`
- Fecha de referencia: `2026-04-28`

## Objetivo

Esta guia resume los endpoints utiles para el panel web administrativo y operativo.

Cobertura principal:

- autenticacion del staff
- dashboard operativo
- catalogos: buses, paradas, rutas, tarifas
- usuarios
- pagos y reversas
- reportes

Fuera del foco del admin web, pero disponibles en la misma API:

- `POST /auth/register`: registro publico de pasajeros
- `/wallet/**`: billetera del pasajero autenticado

## Auth

### `POST /auth/login`

Permite iniciar sesion con usuarios `ADMIN`, `OPERATOR`, `INSPECTOR` o `PASSENGER`.

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

Sugerencia para web admin:

- bloquear acceso al panel si `role` no es `ADMIN`, `OPERATOR` o `INSPECTOR`

### `GET /auth/me`

Sirve para reconstruir sesion desde el JWT ya guardado por frontend.

Response:

```json
{
  "id": "uuid",
  "name": "Administrador",
  "email": "admin@buses.gt",
  "role": "ADMIN",
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

Nota:

- el backend es stateless con JWT; en frontend normalmente basta con eliminar el token local

## Permisos

- `GET /api/v1/**`: `ADMIN`, `OPERATOR`, `INSPECTOR`, `PASSENGER`
- `POST /api/v1/payments`: `ADMIN`, `OPERATOR`, `PASSENGER`
- `POST /api/v1/wallet/top-ups`: `ADMIN`, `OPERATOR`, `PASSENGER`
- otros `POST`, `PUT`, `PATCH` en `/api/v1/**`: `ADMIN`, `OPERATOR`
- `POST /api/v1/auth/login` y `POST /api/v1/auth/register`: publicos

Lectura recomendada para UI:

- `ADMIN`: acceso total
- `OPERATOR`: acceso operativo y de mantenimiento de catalogos
- `INSPECTOR`: solo vistas y consultas `GET`

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
  "path": "/api/v1/users",
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

### Coordenadas

La API mezcla formatos:

- `stops.position`, `routes.stops[].position`, `dashboard.mapMarkers`, `operations-map.*Markers`, `operations-map.routePaths[].points`: `[latitude, longitude]`
- `routes.geometry.coordinates`: `[longitude, latitude]`

## Dashboard operativo

### `GET /dashboard`

Tarjetas y vista rapida inicial del panel.

Response:

```json
{
  "metrics": {
    "activeBuses": 12,
    "registeredRoutes": 8,
    "paymentsToday": 157,
    "revenueToday": 628.0
  },
  "mapMarkers": [
    {
      "id": "uuid",
      "label": "BUS-102",
      "position": [14.6349, -90.5069],
      "status": "ACTIVE"
    }
  ]
}
```

Notas:

- `mapMarkers` devuelve buses ubicados en la parada origen de su ruta
- `paymentsToday` y `revenueToday` usan la fecha UTC del backend

### `GET /operations-map`

Mapa operativo con buses, paradas y trazos de rutas.

Response:

```json
{
  "busMarkers": [],
  "stopMarkers": [],
  "routePaths": [
    {
      "id": "uuid",
      "name": "Ruta 12 Centro",
      "color": "#0b7285",
      "points": [
        [14.6349, -90.5069],
        [14.6401, -90.5032]
      ]
    }
  ]
}
```

## Buses

### `GET /buses`

Filtros:

- `search`: busca por `plate` o `code`
- `status`
- `routeId`
- `page`, `size`, `sort`

Respuesta:

```json
{
  "content": [
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
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

### `GET /buses/by-code/{code}`

Busqueda exacta por codigo. Puede servir para flujos de soporte o caja.
Si frontend genera el QR, el valor recomendado para codificar es `bus.code`.

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

Filtros:

- `search`
- `status`
- `page`, `size`, `sort`

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

Filtros:

- `search`
- `status`
- `page`, `size`, `sort`

En listado:

- `geometry` viene `null`
- `stops[].position` viene `null`

### `GET /routes/{id}`

Devuelve detalle completo con geometria y orden de paradas.

```json
{
  "id": "uuid",
  "name": "Ruta 12 Centro",
  "origin": "Terminal Oriente",
  "destination": "Parque Central",
  "stops": [
    {
      "id": "uuid",
      "code": "P-001",
      "name": "Terminal Oriente",
      "order": 1,
      "position": [14.6349, -90.5069]
    }
  ],
  "geometry": {
    "type": "LineString",
    "coordinates": [
      [-90.5069, 14.6349]
    ]
  },
  "status": "ACTIVE"
}
```

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

Recalcula el `LineString` de la ruta segun el orden actual de sus paradas.

## Tarifas

### `GET /fares`

Filtros:

- `search`
- `status`
- `page`, `size`, `sort`

### `GET /fares/{id}`

### `POST /fares`

```json
{
  "name": "Tarifa urbana",
  "amount": 4.0,
  "validFrom": "2026-01-01",
  "validTo": "2026-12-31",
  "status": "ACTIVE"
}
```

### `PUT /fares/{id}`

### `PATCH /fares/{id}/status`

## Usuarios

### `GET /users`

Filtros:

- `search`
- `role`
- `status`
- `page`, `size`, `sort`

### `GET /users/{id}`

### `POST /users`

```json
{
  "name": "Inspector Centro",
  "email": "inspector@buses.gt",
  "password": "123456",
  "role": "INSPECTOR",
  "status": "ACTIVE"
}
```

Notas:

- `password` es opcional en el DTO, pero para alta operativa conviene enviarlo
- este endpoint puede crear tambien usuarios `PASSENGER`

### `PUT /users/{id}`

```json
{
  "name": "Inspector Centro",
  "email": "inspector@buses.gt",
  "password": "nueva-clave-opcional",
  "role": "INSPECTOR",
  "status": "ACTIVE"
}
```

Notas:

- si envias `password`, el backend reemplaza el hash actual
- si envias `password: null`, conserva la clave existente

### `PATCH /users/{id}/status`

```json
{
  "status": "INACTIVE"
}
```

### `PATCH /users/{id}/role`

```json
{
  "role": "OPERATOR"
}
```

### `POST /users/{id}/reset-password`

```json
{
  "password": "12345678"
}
```

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

Pensado para registrar cobros manuales desde caja, operador o soporte.

```json
{
  "userId": "uuid",
  "busId": "40000000-0000-0000-0000-000000000102",
  "amount": 4.0,
  "method": "CASH",
  "externalReference": "admin-caja-001"
}
```

Reglas:

- `amount >= 0.01`
- si `method = WALLET`, el usuario destino debe ser `PASSENGER`
- si `method = WALLET`, valida saldo disponible
- un `PASSENGER` no puede registrar pagos para otro usuario, pero esto no afecta al staff del admin web

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
  "method": "CASH"
}
```

### `POST /payments/{id}/reverse`

Revierte un pago completado.

```json
{
  "reason": "Cobro duplicado"
}
```

Reglas:

- solo se pueden revertir pagos `COMPLETED`
- si el pago original fue `WALLET`, la reversa reacredita saldo en la billetera del pasajero

## Reportes

### `GET /reports/summary`

Query params:

- `dateFrom` opcional
- `dateTo` opcional

Si no se envian:

- `dateFrom` = inicio del dia actual UTC
- `dateTo` = instante actual

Response:

```json
{
  "activeBuses": 12,
  "registeredRoutes": 8,
  "registeredStops": 54,
  "payments": 157,
  "revenue": 628.0
}
```

### `GET /reports/payments`

Filtros:

- `dateFrom`
- `dateTo`
- `method`
- `status`
- `page`, `size`, `sort`

Devuelve el mismo `PaymentResponse` enriquecido de `GET /payments`.

### `GET /reports/routes`

Response:

```json
[
  {
    "route": "Ruta 12 Centro",
    "stops": 14,
    "assignedBuses": 3,
    "payments": 80,
    "revenue": 320.0
  }
]
```

### `GET /reports/buses`

Response:

```json
[
  {
    "bus": "BUS-102",
    "payments": 33,
    "revenue": 132.0
  }
]
```

### `POST /reports/schedules`

Actualmente registra una programacion simulada y devuelve estado `SCHEDULED`.

```json
{
  "name": "Resumen diario",
  "type": "PAYMENTS",
  "frequency": "DAILY",
  "recipientEmail": "ops@buses.gt"
}
```

Response:

```json
{
  "id": "uuid",
  "name": "Resumen diario",
  "type": "PAYMENTS",
  "frequency": "DAILY",
  "recipientEmail": "ops@buses.gt",
  "status": "SCHEDULED"
}
```

## Endpoints no centrales para Admin Web

### `POST /auth/register`

- publico
- siempre crea usuario `PASSENGER`
- util para app movil o portal publico, no para alta administrativa

### `/wallet/**`

- disponible solo para usuarios `PASSENGER`
- no es un modulo natural del panel admin, salvo que luego construyas una vista de autoservicio para pasajero
