package com.diego.api_buses.controller

import com.diego.api_buses.domain.OperationalStatus
import com.diego.api_buses.domain.PaymentMethod
import com.diego.api_buses.domain.PaymentStatus
import com.diego.api_buses.domain.UserRole
import com.diego.api_buses.dto.*
import com.diego.api_buses.security.AuthService
import com.diego.api_buses.security.UserPrincipal
import com.diego.api_buses.service.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

@RestController
@RequestMapping("/api/v1/auth", "/auth")
class AuthController(private val auth: AuthService) {
    @Operation(summary = "Iniciar sesion")
    @SecurityRequirements
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest) = auth.login(request)

    @Operation(summary = "Registrar pasajero")
    @SecurityRequirements
    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest) = auth.register(request)

    @GetMapping("/me")
    fun me(@AuthenticationPrincipal principal: UserPrincipal) = AuthUserResponse(principal.user.id, principal.user.name, principal.user.email, principal.user.role, principal.user.status)

    @PostMapping("/logout")
    fun logout() = mapOf("success" to true)
}

@RestController
@RequestMapping("/api/v1/buses", "/buses")
class BusController(private val service: BusService) {
    @GetMapping fun list(@RequestParam(required = false) search: String?, @RequestParam(required = false) status: OperationalStatus?, @RequestParam(required = false) routeId: UUID?, @PageableDefault(size = 20) pageable: Pageable) = service.list(search, status, routeId, pageable)
    @GetMapping("/by-code/{code}") fun getByCode(@PathVariable code: String) = service.getByCode(code)
    @GetMapping("/{id}") fun get(@PathVariable id: UUID) = service.get(id)
    @PostMapping fun create(@Valid @RequestBody request: BusRequest) = service.create(request)
    @PutMapping("/{id}") fun update(@PathVariable id: UUID, @Valid @RequestBody request: BusRequest) = service.update(id, request)
    @PatchMapping("/{id}/status") fun status(@PathVariable id: UUID, @Valid @RequestBody request: StatusRequest) = service.updateStatus(id, request.status)
    @PatchMapping("/{id}/route") fun route(@PathVariable id: UUID, @RequestBody request: AssignRouteRequest) = service.assignRoute(id, request.routeId)
    @DeleteMapping("/{id}") fun delete(@PathVariable id: UUID) = service.delete(id)
}

@RestController
@RequestMapping("/api/v1/stops", "/stops")
class StopController(private val service: StopService) {
    @GetMapping fun list(@RequestParam(required = false) search: String?, @RequestParam(required = false) status: OperationalStatus?, @PageableDefault(size = 20) pageable: Pageable) = service.list(search, status, pageable)
    @GetMapping("/{id}") fun get(@PathVariable id: UUID) = service.get(id)
    @PostMapping fun create(@Valid @RequestBody request: StopRequest) = service.create(request)
    @PutMapping("/{id}") fun update(@PathVariable id: UUID, @Valid @RequestBody request: StopRequest) = service.update(id, request)
    @PatchMapping("/{id}/status") fun status(@PathVariable id: UUID, @Valid @RequestBody request: StatusRequest) = service.updateStatus(id, request.status)
    @DeleteMapping("/{id}") fun delete(@PathVariable id: UUID) = service.delete(id)
}

@RestController
@RequestMapping("/api/v1/routes", "/routes")
class RouteController(private val service: RouteService) {
    @GetMapping fun list(@RequestParam(required = false) search: String?, @RequestParam(required = false) status: OperationalStatus?, @PageableDefault(size = 20) pageable: Pageable) = service.list(search, status, pageable)
    @GetMapping("/{id}") fun get(@PathVariable id: UUID) = service.get(id)
    @PostMapping fun create(@Valid @RequestBody request: RouteRequest) = service.create(request)
    @PutMapping("/{id}") fun update(@PathVariable id: UUID, @Valid @RequestBody request: RouteRequest) = service.update(id, request)
    @PatchMapping("/{id}/status") fun status(@PathVariable id: UUID, @Valid @RequestBody request: StatusRequest) = service.updateStatus(id, request.status)
    @PostMapping("/{id}/recalculate-geometry") fun recalculate(@PathVariable id: UUID) = service.recalculateGeometry(id)
    @DeleteMapping("/{id}") fun delete(@PathVariable id: UUID) = service.delete(id)
}

@RestController
@RequestMapping("/api/v1/fares", "/fares")
class FareController(private val service: FareService) {
    @GetMapping fun list(@RequestParam(required = false) search: String?, @RequestParam(required = false) status: OperationalStatus?, @PageableDefault(size = 20) pageable: Pageable) = service.list(search, status, pageable)
    @GetMapping("/{id}") fun get(@PathVariable id: UUID) = service.get(id)
    @PostMapping fun create(@Valid @RequestBody request: FareRequest) = service.create(request)
    @PutMapping("/{id}") fun update(@PathVariable id: UUID, @Valid @RequestBody request: FareRequest) = service.update(id, request)
    @PatchMapping("/{id}/status") fun status(@PathVariable id: UUID, @Valid @RequestBody request: StatusRequest) = service.updateStatus(id, request.status)
    @DeleteMapping("/{id}") fun delete(@PathVariable id: UUID) = service.delete(id)
}

@RestController
@RequestMapping("/api/v1/payments", "/payments")
class PaymentController(private val service: PaymentService) {
    @GetMapping fun list(@RequestParam(required = false) userId: UUID?, @RequestParam(required = false) busId: UUID?, @RequestParam(required = false) status: PaymentStatus?, @RequestParam(required = false) method: PaymentMethod?, @RequestParam(required = false) dateFrom: Instant?, @RequestParam(required = false) dateTo: Instant?, @PageableDefault(size = 20) pageable: Pageable) = service.list(userId, busId, status, method, dateFrom, dateTo, pageable)
    @GetMapping("/{id}") fun get(@PathVariable id: UUID) = service.get(id)
    @PostMapping fun create(@AuthenticationPrincipal principal: UserPrincipal, @Valid @RequestBody request: PaymentRequest) = service.create(request, principal.user)
    @PutMapping("/{id}") fun update(@PathVariable id: UUID, @Valid @RequestBody request: PaymentUpdateRequest) = service.update(id, request)
    @PostMapping("/{id}/reverse") fun reverse(@PathVariable id: UUID, @Valid @RequestBody request: ReversePaymentRequest) = service.reverse(id, request.reason)
    @DeleteMapping("/{id}") fun delete(@PathVariable id: UUID) = mapOf("success" to true).also { service.delete(id) }
}

@RestController
@RequestMapping("/api/v1/users", "/users")
class UserController(private val service: UserService) {
    @GetMapping fun list(@RequestParam(required = false) search: String?, @RequestParam(required = false) role: UserRole?, @RequestParam(required = false) status: OperationalStatus?, @PageableDefault(size = 20) pageable: Pageable) = service.list(search, role, status, pageable)
    @GetMapping("/{id}") fun get(@PathVariable id: UUID) = service.get(id)
    @PostMapping fun create(@Valid @RequestBody request: UserRequest) = service.create(request)
    @PutMapping("/{id}") fun update(@PathVariable id: UUID, @Valid @RequestBody request: UserRequest) = service.update(id, request)
    @PatchMapping("/{id}/status") fun status(@PathVariable id: UUID, @Valid @RequestBody request: StatusRequest) = service.updateStatus(id, request.status)
    @PatchMapping("/{id}/role") fun role(@PathVariable id: UUID, @Valid @RequestBody request: RoleRequest) = service.updateRole(id, request.role)
    @PostMapping("/{id}/reset-password") fun reset(@PathVariable id: UUID, @Valid @RequestBody request: ResetPasswordRequest) = service.resetPassword(id, request.password)
    @DeleteMapping("/{id}") fun delete(@PathVariable id: UUID) = service.delete(id)
}

@RestController
@RequestMapping("/api/v1/wallet", "/wallet")
class WalletController(private val service: WalletService) {
    @GetMapping fun wallet(@AuthenticationPrincipal principal: UserPrincipal) = service.wallet(principal.user)
    @PostMapping("/top-ups") fun topUp(@AuthenticationPrincipal principal: UserPrincipal, @Valid @RequestBody request: WalletTopUpRequest) = service.topUp(principal.user, request)
    @GetMapping("/transactions") fun transactions(@AuthenticationPrincipal principal: UserPrincipal, @PageableDefault(size = 20) pageable: Pageable) = service.transactions(principal.user, pageable)
}

@RestController
@RequestMapping("/api/v1", "")
class OperationalController(private val dashboard: DashboardService, private val reports: ReportService) {
    @GetMapping("/dashboard") fun dashboard() = dashboard.dashboard()
    @GetMapping("/operations-map") fun operationsMap() = dashboard.operationsMap()

    @GetMapping("/reports/summary")
    fun summary(@RequestParam(required = false) dateFrom: Instant?, @RequestParam(required = false) dateTo: Instant?) =
        reports.summary(dateFrom ?: LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC), dateTo ?: Instant.now())

    @GetMapping("/reports/payments")
    fun reportPayments(@RequestParam(required = false) dateFrom: Instant?, @RequestParam(required = false) dateTo: Instant?, @RequestParam(required = false) method: PaymentMethod?, @RequestParam(required = false) status: PaymentStatus?, @PageableDefault(size = 20) pageable: Pageable) =
        reports.payments(dateFrom, dateTo, method, status, pageable)

    @GetMapping("/reports/routes") fun reportRoutes() = reports.routes()
    @GetMapping("/reports/buses") fun reportBuses() = reports.buses()
    @PostMapping("/reports/schedules") fun schedule(@Valid @RequestBody request: ReportScheduleRequest) = reports.schedule(request)
}
