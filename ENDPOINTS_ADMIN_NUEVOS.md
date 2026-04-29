# API Buses - Endpoints Administrativos Nuevos

- Base URL: `https://<host-del-backend>/api/v1`
- Fecha de referencia: `2026-04-29`
- Auth header: `Authorization: Bearer <jwt>`
- Roles requeridos: `ADMIN`, `OPERATOR`

## Resumen

Endpoints agregados recientemente:

- `DELETE /buses/{id}`
- `DELETE /stops/{id}`
- `DELETE /routes/{id}`
- `DELETE /fares/{id}`
- `DELETE /users/{id}`
- `PUT /payments/{id}`
- `DELETE /payments/{id}`

## Regla importante

Los `DELETE` de catalogos no hacen borrado fisico. Hacen baja logica cambiando `status` a `INACTIVE`.

Aplica a:

- `buses`
- `stops`
- `routes`
- `fares`
- `users`

El `DELETE` de `payments` si elimina el registro, pero solo cuando el pago no afecta billetera.

## Buses

### `DELETE /buses/{id}`

Da de baja el bus cambiando su estado a `INACTIVE`.

Response:

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
  "status": "INACTIVE"
}
```

## Paradas

### `DELETE /stops/{id}`

Da de baja la parada cambiando su estado a `INACTIVE`.

Response:

```json
{
  "id": "uuid",
  "code": "P-001",
  "name": "Terminal Oriente",
  "address": "Ingreso Terminal Oriente",
  "position": [14.982000, -89.543000],
  "status": "INACTIVE"
}
```

## Rutas

### `DELETE /routes/{id}`

Da de baja la ruta cambiando su estado a `INACTIVE`.

Response:

```json
{
  "id": "uuid",
  "name": "Ruta 12 Centro",
  "origin": "Terminal Oriente",
  "destination": "Parque Central",
  "stops": [],
  "geometry": {
    "type": "LineString",
    "coordinates": []
  },
  "status": "INACTIVE"
}
```

## Tarifas

### `DELETE /fares/{id}`

Da de baja la tarifa cambiando su estado a `INACTIVE`.

Response:

```json
{
  "id": "uuid",
  "name": "Tarifa Urbana",
  "amount": 4.00,
  "validFrom": "2026-04-01",
  "validTo": "2026-12-31",
  "status": "INACTIVE"
}
```

## Usuarios

### `DELETE /users/{id}`

Da de baja el usuario cambiando su estado a `INACTIVE`.

Response:

```json
{
  "id": "uuid",
  "name": "Administrador",
  "email": "admin@buses.gt",
  "role": "ADMIN",
  "status": "INACTIVE"
}
```

## Pagos

### `PUT /payments/{id}`

Permite correccion administrativa de un pago que no use billetera.

Body:

```json
{
  "userId": "00000000-0000-0000-0000-000000000002",
  "busId": "40000000-0000-0000-0000-000000000118",
  "amount": 5.50,
  "method": "CASH",
  "status": "FAILED",
  "date": "2026-04-29T12:00:00Z",
  "externalReference": "admin-fix-001"
}
```

Reglas:

- no permite editar pagos `WALLET`
- no permite editar pagos con transacciones de billetera asociadas
- no permite cambiar el estado a `REVERSED`; para eso existe `POST /payments/{id}/reverse`

Response:

```json
{
  "id": "uuid",
  "userId": "uuid",
  "user": "Ana Morales",
  "busId": "uuid",
  "bus": "BUS-118",
  "busPlate": "C 118 BAA",
  "routeName": "Ruta 12 Centro",
  "routeOrigin": "Terminal Oriente",
  "routeDestination": "Parque Central",
  "amount": 5.50,
  "date": "2026-04-29T12:00:00Z",
  "status": "FAILED",
  "method": "CASH"
}
```

### `DELETE /payments/{id}`

Elimina fisicamente un pago solo si no afecta billetera.

Reglas:

- no permite eliminar pagos `WALLET`
- no permite eliminar pagos con transacciones de billetera asociadas
- si el pago requiere compensacion de negocio, usa `POST /payments/{id}/reverse`

Response:

```json
{
  "success": true
}
```

## Errores esperados

Cuando una operacion no aplica por negocio, la API responde `400 Bad Request`.

Ejemplo:

```json
{
  "timestamp": "2026-04-29T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Los pagos con impacto en billetera no se pueden eliminar. Usa la reversa.",
  "path": "/api/v1/payments/60000000-0000-0000-0000-000000000003",
  "details": []
}
```
