package com.diego.api_buses.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "users")
class UserEntity(
    @Id
    var id: UUID = UUID.randomUUID(),
    var name: String = "",
    @Column(unique = true, nullable = false)
    var email: String = "",
    var passwordHash: String? = null,
    @Enumerated(EnumType.STRING)
    var role: UserRole = UserRole.PASSENGER,
    @Enumerated(EnumType.STRING)
    var status: OperationalStatus = OperationalStatus.ACTIVE,
    var createdAt: Instant = Instant.now(),
    var updatedAt: Instant = Instant.now(),
) {
    @PrePersist
    fun onCreate() {
        createdAt = Instant.now()
        updatedAt = createdAt
    }

    @PreUpdate
    fun onUpdate() {
        updatedAt = Instant.now()
    }
}

@Entity
@Table(name = "buses")
class BusEntity(
    @Id
    var id: UUID = UUID.randomUUID(),
    @Column(unique = true, nullable = false)
    var plate: String = "",
    @Column(unique = true, nullable = false)
    var code: String = "",
    var capacity: Int = 0,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    var route: RouteEntity? = null,
    @Enumerated(EnumType.STRING)
    var status: OperationalStatus = OperationalStatus.ACTIVE,
    var createdAt: Instant = Instant.now(),
    var updatedAt: Instant = Instant.now(),
) {
    @PrePersist
    fun onCreate() {
        createdAt = Instant.now()
        updatedAt = createdAt
    }

    @PreUpdate
    fun onUpdate() {
        updatedAt = Instant.now()
    }
}

@Entity
@Table(name = "stops")
class StopEntity(
    @Id
    var id: UUID = UUID.randomUUID(),
    @Column(unique = true, nullable = false)
    var code: String = "",
    var name: String = "",
    var address: String = "",
    @Column(precision = 10, scale = 6)
    var latitude: BigDecimal = BigDecimal.ZERO,
    @Column(precision = 10, scale = 6)
    var longitude: BigDecimal = BigDecimal.ZERO,
    @Enumerated(EnumType.STRING)
    var status: OperationalStatus = OperationalStatus.ACTIVE,
    var createdAt: Instant = Instant.now(),
    var updatedAt: Instant = Instant.now(),
) {
    @PrePersist
    fun onCreate() {
        createdAt = Instant.now()
        updatedAt = createdAt
    }

    @PreUpdate
    fun onUpdate() {
        updatedAt = Instant.now()
    }
}

@Entity
@Table(name = "routes")
class RouteEntity(
    @Id
    var id: UUID = UUID.randomUUID(),
    @Column(unique = true, nullable = false)
    var name: String = "",
    @Enumerated(EnumType.STRING)
    var status: OperationalStatus = OperationalStatus.ACTIVE,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_stop_id", nullable = false)
    var originStop: StopEntity = StopEntity(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_stop_id", nullable = false)
    var destinationStop: StopEntity = StopEntity(),
    @Column(columnDefinition = "text", nullable = false)
    var geometry: String = "",
    var createdAt: Instant = Instant.now(),
    var updatedAt: Instant = Instant.now(),
) {
    @PrePersist
    fun onCreate() {
        createdAt = Instant.now()
        updatedAt = createdAt
    }

    @PreUpdate
    fun onUpdate() {
        updatedAt = Instant.now()
    }
}

@Entity
@Table(
    name = "route_stops",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_route_stop_order", columnNames = ["route_id", "stop_order"]),
        UniqueConstraint(name = "uk_route_stop_stop", columnNames = ["route_id", "stop_id"]),
    ],
)
class RouteStopEntity(
    @Id
    var id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    var route: RouteEntity = RouteEntity(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stop_id", nullable = false)
    var stop: StopEntity = StopEntity(),
    @Column(name = "stop_order", nullable = false)
    var stopOrder: Int = 0,
)

@Entity
@Table(name = "fares")
class FareEntity(
    @Id
    var id: UUID = UUID.randomUUID(),
    var name: String = "",
    @Column(precision = 12, scale = 2)
    var amount: BigDecimal = BigDecimal.ZERO,
    var validFrom: LocalDate = LocalDate.now(),
    var validTo: LocalDate = LocalDate.now(),
    @Enumerated(EnumType.STRING)
    var status: OperationalStatus = OperationalStatus.ACTIVE,
    var createdAt: Instant = Instant.now(),
    var updatedAt: Instant = Instant.now(),
) {
    @PrePersist
    fun onCreate() {
        createdAt = Instant.now()
        updatedAt = createdAt
    }

    @PreUpdate
    fun onUpdate() {
        updatedAt = Instant.now()
    }
}

@Entity
@Table(name = "payments")
class PaymentEntity(
    @Id
    var id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: UserEntity = UserEntity(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bus_id", nullable = false)
    var bus: BusEntity = BusEntity(),
    @Column(precision = 12, scale = 2)
    var amount: BigDecimal = BigDecimal.ZERO,
    var date: Instant = Instant.now(),
    @Enumerated(EnumType.STRING)
    var status: PaymentStatus = PaymentStatus.COMPLETED,
    @Enumerated(EnumType.STRING)
    var method: PaymentMethod = PaymentMethod.QR,
    var externalReference: String? = null,
    var reversalReason: String? = null,
    var createdAt: Instant = Instant.now(),
    var updatedAt: Instant = Instant.now(),
) {
    @PrePersist
    fun onCreate() {
        createdAt = Instant.now()
        updatedAt = createdAt
    }

    @PreUpdate
    fun onUpdate() {
        updatedAt = Instant.now()
    }
}
