package com.sequenceiq.provisioning.controller;

import javax.persistence.EntityNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.sequenceiq.provisioning.controller.json.ExceptionResult;
import com.sequenceiq.provisioning.controller.json.ValidationResult;

@ControllerAdvice
public class ExceptionControllerAdvice {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionControllerAdvice.class);

    @ExceptionHandler({ HttpMessageNotReadableException.class, BadRequestException.class })
    public ResponseEntity<ExceptionResult> badRequest(Exception e) {
        LOGGER.error(e.getMessage(), e);
        return new ResponseEntity<>(new ExceptionResult(e.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({ EntityNotFoundException.class })
    public ResponseEntity<ExceptionResult> notFound(Exception e) {
        LOGGER.error(e.getMessage(), e);
        return new ResponseEntity<>(new ExceptionResult(e.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({ MethodArgumentNotValidException.class })
    public ResponseEntity<ValidationResult> validationFailed(MethodArgumentNotValidException e) {
        LOGGER.error(e.getMessage(), e);
        ValidationResult result = new ValidationResult();
        for (FieldError err : e.getBindingResult().getFieldErrors()) {
            result.addValidationError(err.getField(), err.getDefaultMessage());
        }
        return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({ Exception.class })
    public ResponseEntity<ExceptionResult> serverError(Exception e) {
        LOGGER.error(e.getMessage(), e);
        return new ResponseEntity<>(new ExceptionResult("Internal server error"), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
