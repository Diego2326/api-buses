package com.diego.api_buses.repository

import com.diego.api_buses.domain.BusEntity
import com.diego.api_buses.domain.FareEntity
import com.diego.api_buses.domain.OperationalStatus
import com.diego.api_buses.domain.PaymentEntity
import com.diego.api_buses.domain.RouteEntity
import com.diego.api_buses.domain.RouteStopEntity
import com.diego.api_buses.domain.StopEntity
import com.diego.api_buses.domain.UserEntity
import com.diego.api_buses.domain.UserRole
import com.diego.api_buses.domain.WalletTransactionEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

interface UserRepository : JpaRepository<UserEntity, UUID> {
    fun findByEmail(email: String): UserEntity?
    fun existsByEmail(email: String): Boolean

    @Query(
        """
        select u from UserEntity u
        where (:search is null or lower(u.name) like lower(concat('%', :search, '%')) or lower(u.email) like lower(concat('%', :search, '%')))
        and (:role is null or u.role = :role)
        and (:status is null or u.status = :status)
        """,
    )
    fun search(@Param("search") search: String?, @Param("role") role: UserRole?, @Param("status") status: OperationalStatus?, pageable: Pageable): Page<UserEntity>
}

interface BusRepository : JpaRepository<BusEntity, UUID> {
    @Query(
        """
        select b from BusEntity b left join b.route r
        where (:search is null or lower(b.plate) like lower(concat('%', :search, '%')) or lower(b.code) like lower(concat('%', :search, '%')))
        and (:status is null or b.status = :status)
        and (:routeId is null or r.id = :routeId)
        """,
    )
    fun search(search: String?, status: OperationalStatus?, routeId: UUID?, pageable: Pageable): Page<BusEntity>

    fun findByCodeIgnoreCase(code: String): BusEntity?

    fun countByStatus(status: OperationalStatus): Long
    fun countByRouteId(routeId: UUID): Long
}

interface StopRepository : JpaRepository<StopEntity, UUID> {
    @Query(
        """
        select s from StopEntity s
        where (:search is null or lower(s.code) like lower(concat('%', :search, '%')) or lower(s.name) like lower(concat('%', :search, '%')))
        and (:status is null or s.status = :status)
        """,
    )
    fun search(search: String?, status: OperationalStatus?, pageable: Pageable): Page<StopEntity>
}

interface RouteRepository : JpaRepository<RouteEntity, UUID> {
    @Query(
        """
        select r from RouteEntity r
        where (:search is null or lower(r.name) like lower(concat('%', :search, '%')))
        and (:status is null or r.status = :status)
        """,
    )
    fun search(search: String?, status: OperationalStatus?, pageable: Pageable): Page<RouteEntity>
}

interface RouteStopRepository : JpaRepository<RouteStopEntity, UUID> {
    fun findByRouteIdOrderByStopOrderAsc(routeId: UUID): List<RouteStopEntity>
    fun deleteByRouteId(routeId: UUID)
    fun countByRouteId(routeId: UUID): Long
}

interface FareRepository : JpaRepository<FareEntity, UUID> {
    @Query(
        """
        select f from FareEntity f
        where (:search is null or lower(f.name) like lower(concat('%', :search, '%')))
        and (:status is null or f.status = :status)
        """,
    )
    fun search(search: String?, status: OperationalStatus?, pageable: Pageable): Page<FareEntity>
}

interface PaymentRepository : JpaRepository<PaymentEntity, UUID>, JpaSpecificationExecutor<PaymentEntity> {
    fun countByDateBetween(dateFrom: Instant, dateTo: Instant): Long

    @Query("select coalesce(sum(p.amount), 0) from PaymentEntity p where p.status = com.diego.api_buses.domain.PaymentStatus.COMPLETED and p.date between :dateFrom and :dateTo")
    fun revenueBetween(dateFrom: Instant, dateTo: Instant): BigDecimal
}

interface WalletTransactionRepository : JpaRepository<WalletTransactionEntity, UUID> {
    @Query(
        """
        select coalesce(sum(
            case
                when t.type = com.diego.api_buses.domain.WalletTransactionType.TOP_UP then t.amount
                when t.type = com.diego.api_buses.domain.WalletTransactionType.REVERSAL then t.amount
                when t.type = com.diego.api_buses.domain.WalletTransactionType.PAYMENT then -t.amount
                else 0
            end
        ), 0)
        from WalletTransactionEntity t
        where t.user.id = :userId
          and t.status = com.diego.api_buses.domain.WalletTransactionStatus.COMPLETED
        """,
    )
    fun balanceByUserId(@Param("userId") userId: UUID): BigDecimal

    @Query("select t from WalletTransactionEntity t where t.user.id = :userId order by t.date desc")
    fun findByUserId(@Param("userId") userId: UUID, pageable: Pageable): Page<WalletTransactionEntity>

    fun existsByPaymentId(paymentId: UUID): Boolean
}
