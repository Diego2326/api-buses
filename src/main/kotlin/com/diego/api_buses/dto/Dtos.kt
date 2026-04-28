package com.diego.api_buses.dto

import com.diego.api_buses.domain.OperationalStatus
import com.diego.api_buses.domain.PaymentMethod
import com.diego.api_buses.domain.PaymentStatus
import com.diego.api_buses.domain.UserRole
import com.diego.api_buses.domain.WalletTransactionStatus
import com.diego.api_buses.domain.WalletTransactionType
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class PageResponse<T>(val content: List<T>, val page: Int, val size: Int, val totalElements: Long, val totalPages: Int)
data class ErrorDetail(val field: String, val message: String)
data class ErrorResponse(val timestamp: Instant, val status: Int, val error: String, val message: String, val path: String, val details: List<ErrorDetail> = emptyList())

data class LoginRequest(@field:Email val email: String, @field:NotBlank val password: String)
data class RegisterRequest(@field:NotBlank val name: String, @field:Email val email: String, @field:Size(min = 6) val password: String)
data class AuthUserResponse(val id: UUID, val name: String, val email: String, val role: UserRole, val status: OperationalStatus)
data class LoginResponse(val token: String, val user: AuthUserResponse)

data class RouteSummaryResponse(val id: UUID, val name: String, val origin: String, val destination: String)
data class BusResponse(val id: UUID, val plate: String, val code: String, val capacity: Int, val route: RouteSummaryResponse?, val status: OperationalStatus)
data class BusRequest(@field:NotBlank val plate: String, @field:NotBlank val code: String, @field:Min(1) val capacity: Int, val routeId: UUID?, @field:NotNull val status: OperationalStatus)
data class StatusRequest(@field:NotNull val status: OperationalStatus)
data class AssignRouteRequest(val routeId: UUID?)

data class StopResponse(val id: UUID, val code: String, val name: String, val address: String, val position: List<BigDecimal>, val status: OperationalStatus)
data class StopRequest(@field:NotBlank val code: String, @field:NotBlank val name: String, @field:NotBlank val address: String, @field:NotNull val latitude: BigDecimal, @field:NotNull val longitude: BigDecimal, @field:NotNull val status: OperationalStatus)

data class GeometryResponse(val type: String = "LineString", val coordinates: List<List<BigDecimal>>)
data class RouteStopResponse(val id: UUID, val code: String, val name: String, val order: Int, val position: List<BigDecimal>? = null)
data class RouteResponse(val id: UUID, val name: String, val origin: String, val destination: String, val stops: List<RouteStopResponse>, val geometry: GeometryResponse? = null, val status: OperationalStatus)
data class RouteRequest(@field:NotBlank val name: String, @field:Size(min = 2, message = "Debe contener al menos 2 paradas.") val stopIds: List<UUID>, @field:NotNull val status: OperationalStatus)

data class FareResponse(val id: UUID, val name: String, val amount: BigDecimal, val validFrom: LocalDate, val validTo: LocalDate, val status: OperationalStatus)
data class FareRequest(@field:NotBlank val name: String, @field:DecimalMin("0.01") val amount: BigDecimal, @field:NotNull val validFrom: LocalDate, @field:NotNull val validTo: LocalDate, @field:NotNull val status: OperationalStatus)

data class PaymentResponse(
    val id: UUID,
    val userId: UUID,
    val user: String,
    val busId: UUID,
    val bus: String,
    val busPlate: String,
    val routeName: String?,
    val routeOrigin: String?,
    val routeDestination: String?,
    val amount: BigDecimal,
    val date: Instant,
    val status: PaymentStatus,
    val method: PaymentMethod,
)
data class PaymentRequest(@field:NotNull val userId: UUID, @field:NotNull val busId: UUID, @field:DecimalMin("0.01") val amount: BigDecimal, @field:NotNull val method: PaymentMethod, val externalReference: String?)
data class ReversePaymentRequest(@field:NotBlank val reason: String)

data class UserResponse(val id: UUID, val name: String, val email: String, val role: UserRole, val status: OperationalStatus)
data class UserRequest(@field:NotBlank val name: String, @field:Email val email: String, val password: String?, @field:NotNull val role: UserRole, @field:NotNull val status: OperationalStatus)
data class RoleRequest(@field:NotNull val role: UserRole)
data class ResetPasswordRequest(@field:NotBlank val password: String)

data class MarkerResponse(val id: UUID, val label: String, val position: List<BigDecimal>, val status: OperationalStatus)
data class DashboardMetricsResponse(val activeBuses: Long, val registeredRoutes: Long, val paymentsToday: Long, val revenueToday: BigDecimal)
data class DashboardResponse(val metrics: DashboardMetricsResponse, val mapMarkers: List<MarkerResponse>)
data class RoutePathResponse(val id: UUID, val name: String, val color: String, val points: List<List<BigDecimal>>)
data class OperationsMapResponse(val busMarkers: List<MarkerResponse>, val stopMarkers: List<MarkerResponse>, val routePaths: List<RoutePathResponse>)
data class ReportSummaryResponse(val activeBuses: Long, val registeredRoutes: Long, val registeredStops: Long, val payments: Long, val revenue: BigDecimal)
data class RouteMetricResponse(val route: String, val stops: Long, val assignedBuses: Long, val payments: Long, val revenue: BigDecimal)
data class BusMetricResponse(val bus: String, val payments: Long, val revenue: BigDecimal)
data class ReportScheduleRequest(@field:NotBlank val name: String, @field:NotBlank val type: String, @field:NotBlank val frequency: String, @field:Email val recipientEmail: String)
data class ReportScheduleResponse(val id: UUID, val name: String, val type: String, val frequency: String, val recipientEmail: String, val status: String)

data class WalletResponse(val balance: BigDecimal, val currency: String = "GTQ")
data class WalletTopUpRequest(@field:DecimalMin("0.01") val amount: BigDecimal, @field:NotNull val method: PaymentMethod)
data class WalletTopUpResponse(val id: UUID, val amount: BigDecimal, val status: WalletTransactionStatus, val date: Instant)
data class WalletTransactionResponse(val id: UUID, val type: WalletTransactionType, val amount: BigDecimal, val date: Instant, val status: WalletTransactionStatus)
