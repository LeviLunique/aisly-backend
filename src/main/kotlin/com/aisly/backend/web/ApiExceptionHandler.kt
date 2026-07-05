package com.aisly.backend.web

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.net.URI

class NotFoundException(message: String) : RuntimeException(message)
class ForbiddenOperationException(message: String) : RuntimeException(message)
class InvalidRequestException(message: String) : RuntimeException(message)

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(NotFoundException::class)
    fun notFound(error: NotFoundException, request: HttpServletRequest) =
        problem(HttpStatus.NOT_FOUND, error.message ?: "Resource not found", request)

    @ExceptionHandler(ForbiddenOperationException::class)
    fun forbidden(error: ForbiddenOperationException, request: HttpServletRequest) =
        problem(HttpStatus.FORBIDDEN, error.message ?: "Forbidden operation", request)

    @ExceptionHandler(InvalidRequestException::class, IllegalArgumentException::class)
    fun invalid(error: RuntimeException, request: HttpServletRequest) =
        problem(HttpStatus.BAD_REQUEST, error.message ?: "Invalid request", request)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun validation(error: MethodArgumentNotValidException, request: HttpServletRequest): ResponseEntity<ProblemDetail> {
        val message = error.bindingResult.fieldErrors.joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
        return problem(HttpStatus.BAD_REQUEST, message.ifBlank { "Validation failed" }, request)
    }

    private fun problem(status: HttpStatus, detail: String, request: HttpServletRequest): ResponseEntity<ProblemDetail> {
        val body = ProblemDetail.forStatusAndDetail(status, detail)
        body.instance = URI.create(request.requestURI)
        return ResponseEntity.status(status).body(body)
    }
}
