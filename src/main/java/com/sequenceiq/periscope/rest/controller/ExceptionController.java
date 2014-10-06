package com.sequenceiq.periscope.rest.controller;

import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.sequenceiq.periscope.log.Logger;
import com.sequenceiq.periscope.log.PeriscopeLoggerFactory;
import com.sequenceiq.periscope.registry.ConnectionException;
import com.sequenceiq.periscope.registry.QueueSetupException;
import com.sequenceiq.periscope.rest.json.ExceptionMessageJson;
import com.sequenceiq.periscope.rest.json.IdExceptionMessageJson;
import com.sequenceiq.periscope.service.AlarmNotFoundException;
import com.sequenceiq.periscope.service.ClusterNotFoundException;
import com.sequenceiq.periscope.service.NoScalingGroupException;

@ControllerAdvice
public class ExceptionController {

    private static final Logger LOGGER = PeriscopeLoggerFactory.getLogger(ExceptionController.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ExceptionMessageJson> handleIllegalArgException(IllegalArgumentException e) {
        LOGGER.error(LOGGER.NOT_CLUSTER_RELATED, "Unexpected illegal argument exception", e);
        return createExceptionMessage(e.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ExceptionMessageJson> handleNotFoundExceptions(Exception e) {
        LOGGER.error(LOGGER.NOT_CLUSTER_RELATED, "Not found", e);
        String message = e.getMessage();
        return createExceptionMessage(message == null ? "Not found" : message, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ClusterNotFoundException.class)
    public ResponseEntity<IdExceptionMessageJson> handleClusterNotFoundException(ClusterNotFoundException e) {
        return createIdExceptionMessage(e.getId(), "Cluster not found", HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(NoScalingGroupException.class)
    public ResponseEntity<IdExceptionMessageJson> handleNoScalingGroupException(NoScalingGroupException e) {
        return createIdExceptionMessage(e.getId(), e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AlarmNotFoundException.class)
    public ResponseEntity<IdExceptionMessageJson> handleClusterNotFoundException(AlarmNotFoundException e) {
        return createIdExceptionMessage(e.getId(), "Alarm not found", HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ConnectionException.class)
    public ResponseEntity<ExceptionMessageJson> handleConnectionException(ConnectionException e) {
        return createExceptionMessage(e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(QueueSetupException.class)
    public ResponseEntity<ExceptionMessageJson> handleQueueSetupException(QueueSetupException e) {
        return createExceptionMessage(e.getMessage(), HttpStatus.BAD_REQUEST);
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
