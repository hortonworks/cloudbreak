package com.sequenceiq.periscope.rest.controller;

import java.util.NoSuchElementException;

import org.apache.hadoop.yarn.exceptions.YarnException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.sequenceiq.periscope.rest.ExceptionMessage;
import com.sequenceiq.periscope.service.ClusterNotFoundException;

@ControllerAdvice
public class ExceptionController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionController.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ExceptionMessage> handleIllegalArgException(IllegalArgumentException e) {
        LOGGER.error("Unexpected illegal argument exception", e);
        return createExceptionMessage(e.getMessage());
    }

    @ExceptionHandler(YarnException.class)
    public ResponseEntity<ExceptionMessage> handleYarnException(YarnException e) {
        LOGGER.error("Unexpected error during yarn communication", e);
        return createExceptionMessage(e.getMessage());
    }

    @ExceptionHandler({ ClusterNotFoundException.class, NoSuchElementException.class })
    public ResponseEntity<ExceptionMessage> handleNotFoundExceptions(Exception e) {
        LOGGER.error("Not found", e);
        String message = e.getMessage();
        return createExceptionMessage(message == null ? "Not found" : message, HttpStatus.NOT_FOUND);
    }

    private ResponseEntity<ExceptionMessage> createExceptionMessage(String message) {
        return createExceptionMessage(message, HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<ExceptionMessage> createExceptionMessage(String message, HttpStatus statusCode) {
        return new ResponseEntity<>(new ExceptionMessage(message), statusCode);
    }
}
