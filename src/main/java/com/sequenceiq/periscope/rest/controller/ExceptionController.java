package com.sequenceiq.periscope.rest.controller;

import java.text.ParseException;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.sequenceiq.periscope.log.Logger;
import com.sequenceiq.periscope.log.PeriscopeLoggerFactory;
import com.sequenceiq.periscope.rest.json.ExceptionMessageJson;
import com.sequenceiq.periscope.rest.json.IdExceptionMessageJson;
import com.sequenceiq.periscope.service.NotFoundException;
import com.sequenceiq.periscope.service.security.TlsConfigurationException;

@ControllerAdvice
public class ExceptionController {

    private static final Logger LOGGER = PeriscopeLoggerFactory.getLogger(ExceptionController.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ExceptionMessageJson> handleIllegalArgException(IllegalArgumentException e) {
        LOGGER.error(Logger.NOT_CLUSTER_RELATED, "Unexpected illegal argument exception", e);
        return createExceptionMessage(e.getMessage());
    }

    @ExceptionHandler({ NoSuchElementException.class, NotFoundException.class })
    public ResponseEntity<ExceptionMessageJson> handleNotFoundExceptions(Exception e) {
        LOGGER.error(Logger.NOT_CLUSTER_RELATED, "Not found", e);
        String message = e.getMessage();
        return createExceptionMessage(message == null ? "Not found" : message, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ParseException.class)
    public ResponseEntity<ExceptionMessageJson> handleCronExpressionException(ParseException e) {
        return createExceptionMessage(e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ExceptionMessageJson> handleNoScalingGroupException(AccessDeniedException e) {
        return createExceptionMessage(e.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(TlsConfigurationException.class)
    public ResponseEntity<ExceptionMessageJson> handleTlsConfigurationException(TlsConfigurationException e) {
        return createExceptionMessage(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public static ResponseEntity<ExceptionMessageJson> createExceptionMessage(String message) {
        return createExceptionMessage(message, HttpStatus.BAD_REQUEST);
    }

    public static ResponseEntity<ExceptionMessageJson> createExceptionMessage(String message, HttpStatus statusCode) {
        return new ResponseEntity<>(new ExceptionMessageJson(message), statusCode);
    }

    public static ResponseEntity<IdExceptionMessageJson> createIdExceptionMessage(long id, String message, HttpStatus statusCode) {
        return new ResponseEntity<>(new IdExceptionMessageJson(id, message), statusCode);
    }
}
