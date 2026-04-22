package com.diego.api_buses.error

import com.diego.api_buses.dto.ErrorDetail
import com.diego.api_buses.dto.ErrorResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(NotFoundException::class)
    fun notFound(ex: NotFoundException, request: HttpServletRequest) = response(HttpStatus.NOT_FOUND, ex.message ?: "No encontrado.", request.requestURI)

    @ExceptionHandler(BusinessException::class, IllegalArgumentException::class)
    fun business(ex: RuntimeException, request: HttpServletRequest) = response(HttpStatus.BAD_REQUEST, ex.message ?: "Solicitud invalida.", request.requestURI)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun validation(ex: MethodArgumentNotValidException, request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        val details = ex.bindingResult.fieldErrors.map { ErrorDetail(it.field, it.defaultMessage ?: "Valor invalido.") }
        return response(HttpStatus.BAD_REQUEST, "La solicitud contiene errores de validacion.", request.requestURI, details)
    }

    @ExceptionHandler(Exception::class)
    fun generic(ex: Exception, request: HttpServletRequest) = response(HttpStatus.INTERNAL_SERVER_ERROR, ex.message ?: "Error interno.", request.requestURI)

    private fun response(status: HttpStatus, message: String, path: String, details: List<ErrorDetail> = emptyList()) =
        ResponseEntity.status(status).body(ErrorResponse(Instant.now(), status.value(), status.reasonPhrase, message, path, details))
}
