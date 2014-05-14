package com.sequenceiq.provisioning.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.sequenceiq.provisioning.controller.json.ExceptionResult;

@ControllerAdvice
public class ExceptionControllerAdvice {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionControllerAdvice.class);

    @ExceptionHandler({ HttpMessageNotReadableException.class, BadRequestException.class })
    public ResponseEntity<ExceptionResult> badRequest(Exception e) {
        LOGGER.error(e.getMessage(), e);
        return new ResponseEntity<>(new ExceptionResult(e.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({ Exception.class })
    public ResponseEntity<ExceptionResult> serverError(Exception e) {
        LOGGER.error(e.getMessage(), e);
        return new ResponseEntity<>(new ExceptionResult("Internal server error"), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
