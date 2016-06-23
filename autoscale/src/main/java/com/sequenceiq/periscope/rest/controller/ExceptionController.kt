package com.sequenceiq.periscope.rest.controller

import java.text.ParseException
import java.util.NoSuchElementException

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.annotation.ExceptionHandler

import com.sequenceiq.periscope.log.MDCBuilder
import com.sequenceiq.periscope.rest.json.ExceptionMessageJson
import com.sequenceiq.periscope.rest.json.IdExceptionMessageJson
import com.sequenceiq.periscope.service.NotFoundException
import com.sequenceiq.periscope.service.security.TlsConfigurationException

//@ControllerAdvice
class ExceptionController {

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgException(e: IllegalArgumentException): ResponseEntity<ExceptionMessageJson> {
        MDCBuilder.buildMdcContext()
        LOGGER.error("Unexpected illegal argument exception", e)
        return createExceptionMessage(e.message)
    }

    @ExceptionHandler(NoSuchElementException::class, NotFoundException::class)
    fun handleNotFoundExceptions(e: Exception): ResponseEntity<ExceptionMessageJson> {
        MDCBuilder.buildMdcContext()
        LOGGER.error("Not found", e)
        val message = e.message
        return createExceptionMessage(message ?: "Not found", HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(ParseException::class)
    fun handleCronExpressionException(e: ParseException): ResponseEntity<ExceptionMessageJson> {
        MDCBuilder.buildMdcContext()
        return createExceptionMessage(e.message, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleNoScalingGroupException(e: AccessDeniedException): ResponseEntity<ExceptionMessageJson> {
        MDCBuilder.buildMdcContext()
        return createExceptionMessage(e.message, HttpStatus.FORBIDDEN)
    }

    @ExceptionHandler(TlsConfigurationException::class)
    fun handleTlsConfigurationException(e: TlsConfigurationException): ResponseEntity<ExceptionMessageJson> {
        MDCBuilder.buildMdcContext()
        return createExceptionMessage(e.message, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(ExceptionController::class.java)

        @JvmOverloads fun createExceptionMessage(message: String, statusCode: HttpStatus = HttpStatus.BAD_REQUEST): ResponseEntity<ExceptionMessageJson> {
            return ResponseEntity(ExceptionMessageJson(message), statusCode)
        }

        fun createIdExceptionMessage(id: Long, message: String, statusCode: HttpStatus): ResponseEntity<IdExceptionMessageJson> {
            return ResponseEntity(IdExceptionMessageJson(id, message), statusCode)
        }
    }
}
