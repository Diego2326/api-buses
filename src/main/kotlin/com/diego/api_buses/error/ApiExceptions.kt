package com.diego.api_buses.error

class NotFoundException(message: String) : RuntimeException(message)
class BusinessException(message: String) : RuntimeException(message)
