package com.sequenceiq.provisioning;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ExceptionControllerAdvice {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionControllerAdvice.class);

    @ExceptionHandler({ Exception.class })
    public ResponseEntity<ExceptionJson> serverError(Exception e) {
        LOGGER.error(e.getMessage(), e);
        return new ResponseEntity<>(new ExceptionJson("Internal server error"), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
