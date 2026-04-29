package com.diego.api_buses.service

import com.diego.api_buses.domain.BusEntity
import com.diego.api_buses.domain.FareEntity
import com.diego.api_buses.domain.OperationalStatus
import com.diego.api_buses.domain.PaymentEntity
import com.diego.api_buses.domain.PaymentMethod
import com.diego.api_buses.domain.PaymentStatus
import com.diego.api_buses.domain.RouteEntity
import com.diego.api_buses.domain.RouteStopEntity
import com.diego.api_buses.domain.StopEntity
import com.diego.api_buses.domain.UserEntity
import com.diego.api_buses.domain.UserRole
import com.diego.api_buses.domain.WalletTransactionEntity
import com.diego.api_buses.domain.WalletTransactionStatus
import com.diego.api_buses.domain.WalletTransactionType
import com.diego.api_buses.dto.*
import com.diego.api_buses.error.BusinessException
import com.diego.api_buses.error.NotFoundException
import com.diego.api_buses.repository.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

fun <T : Any, R> Page<T>.toPageResponse(mapper: (T) -> R) = PageResponse(content.map(mapper), number, size, totalElements, totalPages)

private fun <T> java.util.Optional<T>.orNotFound(message: String): T = orElseThrow { NotFoundException(message) }
private fun normalizeSearch(search: String?): String = search?.trim()?.takeIf { it.isNotEmpty() } ?: ""

@Service
class GeometryService {
    fun calculate(stops: List<StopEntity>): String {
        val coordinates = stops.joinToString(",") { "[${it.longitude},${it.latitude}]" }
        return """{"type":"LineString","coordinates":[$coordinates]}"""
    }

    fun parse(geometry: String): GeometryResponse {
        val coordinates = Regex("""\[\s*(-?\d+(?:\.\d+)?)\s*,\s*(-?\d+(?:\.\d+)?)\s*]""")
            .findAll(geometry.substringAfter("\"coordinates\""))
            .map { listOf(BigDecimal(it.groupValues[1]), BigDecimal(it.groupValues[2])) }
            .toList()
        return GeometryResponse(coordinates = coordinates)
    }
}

@Service
class BusService(private val buses: BusRepository, private val routes: RouteRepository) {
    fun list(search: String?, status: OperationalStatus?, routeId: UUID?, pageable: Pageable) =
        buses.search(normalizeSearch(search), status, routeId, pageable).toPageResponse(::toResponse)
    fun get(id: UUID) = toResponse(buses.findById(id).orNotFound("Bus no encontrado."))
    fun getByCode(code: String) = toResponse(buses.findByCodeIgnoreCase(code) ?: throw NotFoundException("Bus no encontrado."))

    @Transactional
    fun create(request: BusRequest) = toResponse(buses.save(BusEntity(plate = request.plate, code = request.code, capacity = request.capacity, route = resolveRoute(request.routeId), status = request.status)))

    @Transactional
    fun update(id: UUID, request: BusRequest): BusResponse {
        val bus = buses.findById(id).orNotFound("Bus no encontrado.")
        bus.plate = request.plate
        bus.code = request.code
        bus.capacity = request.capacity
        bus.route = resolveRoute(request.routeId)
        bus.status = request.status
        return toResponse(bus)
    }

    @Transactional
    fun updateStatus(id: UUID, status: OperationalStatus): BusResponse {
        val bus = buses.findById(id).orNotFound("Bus no encontrado.")
        bus.status = status
        return toResponse(bus)
    }

    @Transactional
    fun assignRoute(id: UUID, routeId: UUID?): BusResponse {
        val bus = buses.findById(id).orNotFound("Bus no encontrado.")
        bus.route = resolveRoute(routeId)
        return toResponse(bus)
    }

    @Transactional
    fun delete(id: UUID): BusResponse {
        val bus = buses.findById(id).orNotFound("Bus no encontrado.")
        bus.status = OperationalStatus.INACTIVE
        return toResponse(bus)
    }

    private fun resolveRoute(routeId: UUID?): RouteEntity? {
        val route = routeId?.let { routes.findById(it).orNotFound("Ruta no encontrada.") }
        if (route?.status == OperationalStatus.SUSPENDED) throw BusinessException("No se puede asignar un bus a una ruta suspendida.")
        return route
    }

    private fun toResponse(bus: BusEntity) = BusResponse(
        bus.id,
        bus.plate,
        bus.code,
        bus.capacity,
        bus.route?.let { RouteSummaryResponse(it.id, it.name, it.originStop.name, it.destinationStop.name) },
        bus.status,
    )
}

@Service
class StopService(private val stops: StopRepository) {
    fun list(search: String?, status: OperationalStatus?, pageable: Pageable) =
        stops.search(normalizeSearch(search), status, pageable).toPageResponse(::toResponse)
    fun get(id: UUID) = toResponse(stops.findById(id).orNotFound("Parada no encontrada."))

    @Transactional
    fun create(request: StopRequest) = toResponse(stops.save(StopEntity(code = request.code, name = request.name, address = request.address, latitude = request.latitude, longitude = request.longitude, status = request.status)))

    @Transactional
    fun update(id: UUID, request: StopRequest): StopResponse {
        val stop = stops.findById(id).orNotFound("Parada no encontrada.")
        stop.code = request.code
        stop.name = request.name
        stop.address = request.address
        stop.latitude = request.latitude
        stop.longitude = request.longitude
        stop.status = request.status
        return toResponse(stop)
    }

    @Transactional
    fun updateStatus(id: UUID, status: OperationalStatus): StopResponse {
        val stop = stops.findById(id).orNotFound("Parada no encontrada.")
        stop.status = status
        return toResponse(stop)
    }

    @Transactional
    fun delete(id: UUID): StopResponse {
        val stop = stops.findById(id).orNotFound("Parada no encontrada.")
        stop.status = OperationalStatus.INACTIVE
        return toResponse(stop)
    }

    fun toResponse(stop: StopEntity) = StopResponse(stop.id, stop.code, stop.name, stop.address, listOf(stop.latitude, stop.longitude), stop.status)
}

@Service
class RouteService(private val routes: RouteRepository, private val stops: StopRepository, private val routeStops: RouteStopRepository, private val geometry: GeometryService) {
    fun list(search: String?, status: OperationalStatus?, pageable: Pageable) =
        routes.search(normalizeSearch(search), status, pageable).toPageResponse { toResponse(it, includeGeometry = false) }
    fun get(id: UUID) = toResponse(routes.findById(id).orNotFound("Ruta no encontrada."), includeGeometry = true)

    @Transactional
    fun create(request: RouteRequest): RouteResponse {
        val orderedStops = resolveStops(request.stopIds)
        val route = routes.save(RouteEntity(name = request.name, status = request.status, originStop = orderedStops.first(), destinationStop = orderedStops.last(), geometry = geometry.calculate(orderedStops)))
        saveRouteStops(route, orderedStops)
        return toResponse(route, includeGeometry = true)
    }

    @Transactional
    fun update(id: UUID, request: RouteRequest): RouteResponse {
        val route = routes.findById(id).orNotFound("Ruta no encontrada.")
        val orderedStops = resolveStops(request.stopIds)
        route.name = request.name
        route.status = request.status
        route.originStop = orderedStops.first()
        route.destinationStop = orderedStops.last()
        route.geometry = geometry.calculate(orderedStops)
        routeStops.deleteByRouteId(route.id)
        saveRouteStops(route, orderedStops)
        return toResponse(route, includeGeometry = true)
    }

    @Transactional
    fun updateStatus(id: UUID, status: OperationalStatus): RouteResponse {
        val route = routes.findById(id).orNotFound("Ruta no encontrada.")
        route.status = status
        return toResponse(route, includeGeometry = true)
    }

    @Transactional
    fun recalculateGeometry(id: UUID): RouteResponse {
        val route = routes.findById(id).orNotFound("Ruta no encontrada.")
        val orderedStops = routeStops.findByRouteIdOrderByStopOrderAsc(id).map { it.stop }
        route.geometry = geometry.calculate(orderedStops)
        return toResponse(route, includeGeometry = true)
    }

    @Transactional
    fun delete(id: UUID): RouteResponse {
        val route = routes.findById(id).orNotFound("Ruta no encontrada.")
        route.status = OperationalStatus.INACTIVE
        return toResponse(route, includeGeometry = true)
    }

    fun toResponse(route: RouteEntity, includeGeometry: Boolean): RouteResponse {
        val ordered = routeStops.findByRouteIdOrderByStopOrderAsc(route.id)
        return RouteResponse(
            id = route.id,
            name = route.name,
            origin = route.originStop.name,
            destination = route.destinationStop.name,
            stops = ordered.map { RouteStopResponse(it.stop.id, it.stop.code, it.stop.name, it.stopOrder, if (includeGeometry) listOf(it.stop.latitude, it.stop.longitude) else null) },
            geometry = if (includeGeometry) geometry.parse(route.geometry) else null,
            status = route.status,
        )
    }

    private fun resolveStops(ids: List<UUID>): List<StopEntity> {
        if (ids.size < 2) throw BusinessException("La ruta debe tener al menos 2 paradas.")
        if (ids.distinct().size != ids.size) throw BusinessException("No se permite repetir una parada dentro de la misma ruta.")
        val found = stops.findAllById(ids).associateBy { it.id }
        return ids.map { found[it] ?: throw NotFoundException("Parada no encontrada: $it") }
    }

    private fun saveRouteStops(route: RouteEntity, orderedStops: List<StopEntity>) {
        routeStops.saveAll(orderedStops.mapIndexed { index, stop -> RouteStopEntity(route = route, stop = stop, stopOrder = index + 1) })
    }
}

@Service
class FareService(private val fares: FareRepository) {
    fun list(search: String?, status: OperationalStatus?, pageable: Pageable) =
        fares.search(normalizeSearch(search), status, pageable).toPageResponse(::toResponse)
    fun get(id: UUID) = toResponse(fares.findById(id).orNotFound("Tarifa no encontrada."))
    @Transactional fun create(request: FareRequest) = toResponse(fares.save(FareEntity(name = request.name, amount = request.amount, validFrom = request.validFrom, validTo = request.validTo, status = request.status)))
    @Transactional fun update(id: UUID, request: FareRequest): FareResponse {
        val fare = fares.findById(id).orNotFound("Tarifa no encontrada.")
        fare.name = request.name; fare.amount = request.amount; fare.validFrom = request.validFrom; fare.validTo = request.validTo; fare.status = request.status
        return toResponse(fare)
    }
    @Transactional fun updateStatus(id: UUID, status: OperationalStatus): FareResponse {
        val fare = fares.findById(id).orNotFound("Tarifa no encontrada.")
        fare.status = status
        return toResponse(fare)
    }
    @Transactional fun delete(id: UUID): FareResponse {
        val fare = fares.findById(id).orNotFound("Tarifa no encontrada.")
        fare.status = OperationalStatus.INACTIVE
        return toResponse(fare)
    }
    private fun toResponse(fare: FareEntity) = FareResponse(fare.id, fare.name, fare.amount, fare.validFrom, fare.validTo, fare.status)
}

@Service
class PaymentService(
    private val payments: PaymentRepository,
    private val users: UserRepository,
    private val buses: BusRepository,
    private val walletTransactions: WalletTransactionRepository,
) {
    fun list(userId: UUID?, busId: UUID?, status: PaymentStatus?, method: PaymentMethod?, dateFrom: Instant?, dateTo: Instant?, pageable: Pageable) = payments.search(userId, busId, status, method, dateFrom, dateTo, pageable).toPageResponse(::toResponse)
    fun get(id: UUID) = toResponse(payments.findById(id).orNotFound("Pago no encontrado."))

    @Transactional
    fun create(request: PaymentRequest, actor: UserEntity): PaymentResponse {
        val user = resolvePaymentUser(request.userId, actor)
        val bus = buses.findById(request.busId).orNotFound("Bus no encontrado.")
        if (request.method == PaymentMethod.WALLET) {
            ensureWalletOwner(user)
            val balance = walletTransactions.balanceByUserId(user.id)
            if (balance < request.amount) throw BusinessException("Saldo insuficiente en billetera.")
        }

        val payment = payments.save(
            PaymentEntity(user = user, bus = bus, amount = request.amount, method = request.method, externalReference = request.externalReference, status = PaymentStatus.COMPLETED),
        )

        if (request.method == PaymentMethod.WALLET) {
            walletTransactions.save(
                WalletTransactionEntity(
                    user = user,
                    amount = request.amount,
                    type = WalletTransactionType.PAYMENT,
                    status = WalletTransactionStatus.COMPLETED,
                    method = request.method,
                    payment = payment,
                    date = payment.date,
                ),
            )
        }

        return toResponse(payment)
    }

    @Transactional
    fun update(id: UUID, request: PaymentUpdateRequest): PaymentResponse {
        val payment = payments.findById(id).orNotFound("Pago no encontrado.")
        if (payment.method == PaymentMethod.WALLET || request.method == PaymentMethod.WALLET || walletTransactions.existsByPaymentId(id)) {
            throw BusinessException("Los pagos con billetera no se pueden editar administrativamente. Usa la reversa y crea un nuevo pago.")
        }
        if (request.status == PaymentStatus.REVERSED) {
            throw BusinessException("Para marcar un pago como revertido usa el endpoint de reversa.")
        }

        payment.user = users.findById(request.userId).orNotFound("Usuario no encontrado.")
        payment.bus = buses.findById(request.busId).orNotFound("Bus no encontrado.")
        payment.amount = request.amount
        payment.method = request.method
        payment.status = request.status
        payment.date = request.date
        payment.externalReference = request.externalReference
        return toResponse(payment)
    }

    @Transactional
    fun reverse(id: UUID, reason: String): PaymentResponse {
        val payment = payments.findById(id).orNotFound("Pago no encontrado.")
        if (payment.status != PaymentStatus.COMPLETED) throw BusinessException("Solo se pueden revertir pagos completados.")
        payment.status = PaymentStatus.REVERSED
        payment.reversalReason = reason
        if (payment.method == PaymentMethod.WALLET) {
            ensureWalletOwner(payment.user)
            walletTransactions.save(
                WalletTransactionEntity(
                    user = payment.user,
                    amount = payment.amount,
                    type = WalletTransactionType.REVERSAL,
                    status = WalletTransactionStatus.COMPLETED,
                    method = payment.method,
                    payment = payment,
                    date = Instant.now(),
                ),
            )
        }
        return toResponse(payment)
    }

    @Transactional
    fun delete(id: UUID) {
        val payment = payments.findById(id).orNotFound("Pago no encontrado.")
        if (payment.method == PaymentMethod.WALLET || walletTransactions.existsByPaymentId(id)) {
            throw BusinessException("Los pagos con impacto en billetera no se pueden eliminar. Usa la reversa.")
        }
        payments.delete(payment)
    }

    fun toResponse(payment: PaymentEntity) = PaymentResponse(
        id = payment.id,
        userId = payment.user.id,
        user = payment.user.name,
        busId = payment.bus.id,
        bus = payment.bus.code,
        busPlate = payment.bus.plate,
        routeName = payment.bus.route?.name,
        routeOrigin = payment.bus.route?.originStop?.name,
        routeDestination = payment.bus.route?.destinationStop?.name,
        amount = payment.amount,
        date = payment.date,
        status = payment.status,
        method = payment.method,
    )

    private fun resolvePaymentUser(userId: UUID, actor: UserEntity): UserEntity {
        if (actor.role == UserRole.PASSENGER && actor.id != userId) {
            throw BusinessException("No puedes registrar pagos para otro usuario.")
        }
        return users.findById(userId).orNotFound("Usuario no encontrado.")
    }

    private fun ensureWalletOwner(user: UserEntity) {
        if (user.role != UserRole.PASSENGER) throw BusinessException("La billetera solo aplica para usuarios pasajeros.")
    }
}

@Service
class UserService(private val users: UserRepository, private val passwordEncoder: PasswordEncoder) {
    fun list(search: String?, role: UserRole?, status: OperationalStatus?, pageable: Pageable) =
        users.search(normalizeSearch(search), role, status, pageable).toPageResponse(::toResponse)
    fun get(id: UUID) = toResponse(users.findById(id).orNotFound("Usuario no encontrado."))
    @Transactional fun create(request: UserRequest) = toResponse(users.save(UserEntity(name = request.name, email = request.email, passwordHash = request.password?.let(passwordEncoder::encode), role = request.role, status = request.status)))
    @Transactional fun update(id: UUID, request: UserRequest): UserResponse {
        val user = users.findById(id).orNotFound("Usuario no encontrado.")
        user.name = request.name; user.email = request.email; user.role = request.role; user.status = request.status
        request.password?.let { user.passwordHash = passwordEncoder.encode(it) }
        return toResponse(user)
    }
    @Transactional fun updateStatus(id: UUID, status: OperationalStatus): UserResponse {
        val user = users.findById(id).orNotFound("Usuario no encontrado."); user.status = status; return toResponse(user)
    }
    @Transactional fun updateRole(id: UUID, role: UserRole): UserResponse {
        val user = users.findById(id).orNotFound("Usuario no encontrado."); user.role = role; return toResponse(user)
    }
    @Transactional fun resetPassword(id: UUID, password: String): UserResponse {
        val user = users.findById(id).orNotFound("Usuario no encontrado."); user.passwordHash = passwordEncoder.encode(password); return toResponse(user)
    }
    @Transactional fun delete(id: UUID): UserResponse {
        val user = users.findById(id).orNotFound("Usuario no encontrado.")
        user.status = OperationalStatus.INACTIVE
        return toResponse(user)
    }
    fun toResponse(user: UserEntity) = UserResponse(user.id, user.name, user.email, user.role, user.status)
}

@Service
class WalletService(private val users: UserRepository, private val walletTransactions: WalletTransactionRepository) {
    fun wallet(principal: UserEntity): WalletResponse {
        val user = resolvePassenger(principal.id)
        return WalletResponse(balance = walletTransactions.balanceByUserId(user.id))
    }

    @Transactional
    fun topUp(principal: UserEntity, request: WalletTopUpRequest): WalletTopUpResponse {
        val user = resolvePassenger(principal.id)
        if (request.method == PaymentMethod.WALLET) throw BusinessException("No se puede recargar la billetera usando el mismo metodo WALLET.")
        val transaction = walletTransactions.save(
            WalletTransactionEntity(
                user = user,
                amount = request.amount,
                type = WalletTransactionType.TOP_UP,
                status = WalletTransactionStatus.COMPLETED,
                method = request.method,
                date = Instant.now(),
            ),
        )
        return WalletTopUpResponse(transaction.id, transaction.amount, transaction.status, transaction.date)
    }

    fun transactions(principal: UserEntity, pageable: Pageable) =
        walletTransactions.findByUserId(resolvePassenger(principal.id).id, pageable).toPageResponse(::toResponse)

    private fun toResponse(transaction: WalletTransactionEntity) =
        WalletTransactionResponse(transaction.id, transaction.type, transaction.amount, transaction.date, transaction.status)

    private fun resolvePassenger(userId: UUID): UserEntity {
        val user = users.findById(userId).orNotFound("Usuario no encontrado.")
        if (user.role != UserRole.PASSENGER) throw BusinessException("La billetera solo esta disponible para pasajeros.")
        return user
    }
}

@Service
class DashboardService(private val buses: BusRepository, private val routes: RouteRepository, private val stops: StopRepository, private val payments: PaymentRepository, private val routeService: RouteService, private val geometry: GeometryService) {
    fun dashboard(): DashboardResponse {
        val start = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC)
        val end = start.plusSeconds(86_399)
        return DashboardResponse(DashboardMetricsResponse(buses.countByStatus(OperationalStatus.ACTIVE), routes.count(), payments.countByDateBetween(start, end), payments.revenueBetween(start, end)), busMarkers())
    }

    fun operationsMap(): OperationsMapResponse {
        val stopMarkers = stops.findAll().map { MarkerResponse(it.id, "${it.code} - ${it.name}", listOf(it.latitude, it.longitude), it.status) }
        val paths = routes.findAll().map {
            val points = geometry.parse(it.geometry).coordinates.map { coordinate -> listOf(coordinate[1], coordinate[0]) }
            RoutePathResponse(it.id, it.name, "#0b7285", points)
        }
        return OperationsMapResponse(busMarkers(), stopMarkers, paths)
    }

    private fun busMarkers() = buses.findAll().mapNotNull { bus ->
        val stop = bus.route?.originStop ?: return@mapNotNull null
        MarkerResponse(bus.id, bus.code, listOf(stop.latitude, stop.longitude), bus.status)
    }
}

@Service
class ReportService(private val buses: BusRepository, private val routes: RouteRepository, private val stops: StopRepository, private val routeStops: RouteStopRepository, private val payments: PaymentRepository) {
    fun summary(dateFrom: Instant, dateTo: Instant) = ReportSummaryResponse(buses.countByStatus(OperationalStatus.ACTIVE), routes.count(), stops.count(), payments.countByDateBetween(dateFrom, dateTo), payments.revenueBetween(dateFrom, dateTo))
    fun payments(dateFrom: Instant?, dateTo: Instant?, method: PaymentMethod?, status: PaymentStatus?, pageable: Pageable) =
        payments.search(null, null, status, method, dateFrom, dateTo, pageable).toPageResponse {
            PaymentResponse(
                id = it.id,
                userId = it.user.id,
                user = it.user.name,
                busId = it.bus.id,
                bus = it.bus.code,
                busPlate = it.bus.plate,
                routeName = it.bus.route?.name,
                routeOrigin = it.bus.route?.originStop?.name,
                routeDestination = it.bus.route?.destinationStop?.name,
                amount = it.amount,
                date = it.date,
                status = it.status,
                method = it.method,
            )
        }
    fun routes(): List<RouteMetricResponse> {
        val completedPayments = payments.findAll().filter { it.status == PaymentStatus.COMPLETED }
        return routes.findAll().map { route ->
            val routePayments = completedPayments.filter { it.bus.route?.id == route.id }
            RouteMetricResponse(route.name, routeStops.countByRouteId(route.id), buses.countByRouteId(route.id), routePayments.size.toLong(), routePayments.fold(BigDecimal.ZERO) { acc, payment -> acc + payment.amount })
        }
    }

    fun buses(): List<BusMetricResponse> {
        val completedPayments = payments.findAll().filter { it.status == PaymentStatus.COMPLETED }
        return buses.findAll().map { bus ->
            val busPayments = completedPayments.filter { it.bus.id == bus.id }
            BusMetricResponse(bus.code, busPayments.size.toLong(), busPayments.fold(BigDecimal.ZERO) { acc, payment -> acc + payment.amount })
        }
    }

    fun schedule(request: ReportScheduleRequest) = ReportScheduleResponse(UUID.randomUUID(), request.name, request.type, request.frequency, request.recipientEmail, "SCHEDULED")
}
