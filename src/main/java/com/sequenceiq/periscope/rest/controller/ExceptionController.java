package com.sequenceiq.periscope.rest.controller;

import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.sequenceiq.periscope.rest.json.ClusterJson;
import com.sequenceiq.periscope.rest.json.ExceptionMessageJson;
import com.sequenceiq.periscope.service.ClusterNotFoundException;

@ControllerAdvice
public class ExceptionController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionController.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ExceptionMessageJson> handleIllegalArgException(IllegalArgumentException e) {
        LOGGER.error("Unexpected illegal argument exception", e);
        return createExceptionMessage(e.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ExceptionMessageJson> handleNotFoundExceptions(Exception e) {
        LOGGER.error("Not found", e);
        String message = e.getMessage();
        return createExceptionMessage(message == null ? "Not found" : message, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ClusterNotFoundException.class)
    public ResponseEntity<ClusterJson> handleClusterNotFoundException(ClusterNotFoundException e) {
        return new ResponseEntity<>(ClusterJson.emptyJson().withId(e.getId()), HttpStatus.NOT_FOUND);
    }

    public static ResponseEntity<ExceptionMessageJson> createExceptionMessage(String message) {
        return createExceptionMessage(message, HttpStatus.BAD_REQUEST);
    }

    public static ResponseEntity<ExceptionMessageJson> createExceptionMessage(String message, HttpStatus statusCode) {
        return new ResponseEntity<>(new ExceptionMessageJson(message), statusCode);
    }
}
