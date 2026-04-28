package com.diego.api_buses.domain

enum class OperationalStatus {
    ACTIVE,
    INACTIVE,
    MAINTENANCE,
    SUSPENDED,
}

enum class PaymentStatus {
    COMPLETED,
    PENDING,
    FAILED,
    REVERSED,
}

enum class PaymentMethod {
    CARD,
    QR,
    CASH,
    WALLET,
}

enum class UserRole {
    ADMIN,
    OPERATOR,
    INSPECTOR,
    PASSENGER,
}

enum class WalletTransactionType {
    TOP_UP,
    PAYMENT,
    REVERSAL,
}

enum class WalletTransactionStatus {
    COMPLETED,
    FAILED,
}
