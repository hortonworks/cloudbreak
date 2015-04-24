package com.sequenceiq.cloudbreak.controller;

import java.nio.file.AccessDeniedException;

import javax.persistence.EntityNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.sequenceiq.cloudbreak.controller.json.ExceptionResult;
import com.sequenceiq.cloudbreak.controller.json.ValidationResult;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException;
import com.sequenceiq.cloudbreak.service.subscription.SubscriptionAlreadyExistException;

@ControllerAdvice
public class ExceptionControllerAdvice {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionControllerAdvice.class);

    @ExceptionHandler({ AuthenticationCredentialsNotFoundException.class })
    public ResponseEntity<ExceptionResult> unauthorized(Exception e) {
        MDCBuilder.buildMdcContext();
        LOGGER.error(e.getMessage(), e);
        return new ResponseEntity<>(new ExceptionResult(e.getMessage()), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler({ TypeMismatchException.class, HttpMessageNotReadableException.class, BadRequestException.class })
    public ResponseEntity<ExceptionResult> badRequest(Exception e) {
        MDCBuilder.buildMdcContext();
        LOGGER.error(e.getMessage(), e);
        return new ResponseEntity<>(new ExceptionResult(e.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({ AccessDeniedException.class, org.springframework.security.access.AccessDeniedException.class })
    public ResponseEntity<ExceptionResult> accessDenied(Exception e) {
        LOGGER.error(e.getMessage(), e);
        return new ResponseEntity<>(new ExceptionResult(e.getMessage()), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler({ NotFoundException.class, EntityNotFoundException.class })
    public ResponseEntity<ExceptionResult> notFound(Exception e) {
        MDCBuilder.buildMdcContext();
        LOGGER.error(e.getMessage());
        return new ResponseEntity<>(new ExceptionResult(e.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({ MethodArgumentNotValidException.class })
    public ResponseEntity<ValidationResult> validationFailed(MethodArgumentNotValidException e) {
        MDCBuilder.buildMdcContext();
        LOGGER.error(e.getMessage());
        ValidationResult result = new ValidationResult();
        for (FieldError err : e.getBindingResult().getFieldErrors()) {
            result.addValidationError(err.getField(), err.getDefaultMessage());
        }
        return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({ HttpRequestMethodNotSupportedException.class })
    public ResponseEntity<ExceptionResult> httpRequestMethodNotSupportedExceptionError(Exception e) {
        MDCBuilder.buildMdcContext();
        LOGGER.error(e.getMessage(), e);
        return new ResponseEntity<>(new ExceptionResult("The requested http method is not supported on the resource."), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({ HttpMediaTypeNotSupportedException.class })
    public ResponseEntity<ExceptionResult> httpMediaTypeNotSupported(Exception e) {
        MDCBuilder.buildMdcContext();
        LOGGER.error(e.getMessage());
        return new ResponseEntity<>(new ExceptionResult(e.getMessage()), HttpStatus.NOT_ACCEPTABLE);
    }

    @ExceptionHandler({ SubscriptionAlreadyExistException.class })
    public ResponseEntity<ExceptionResult> subscriptionExists(Exception e) {
        LOGGER.info(e.getMessage());
        return new ResponseEntity<>(new ExceptionResult(e.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler({ Exception.class, RuntimeException.class })
    public ResponseEntity<ExceptionResult> serverError(Exception e) {
        MDCBuilder.buildMdcContext();
        LOGGER.error(e.getMessage(), e);
        return new ResponseEntity<>(new ExceptionResult("Internal server error"), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler({ DuplicateKeyValueException.class })
    public ResponseEntity<ExceptionResult> duplicatedName(DuplicateKeyValueException e) {
        MDCBuilder.buildMdcContext();
        LOGGER.error(e.getMessage(), e);
        return new ResponseEntity<>(
                new ExceptionResult(String.format("The %s name '%s' is already taken, please choose a different one",
                        e.getResourceType().toString().toLowerCase(),
                        e.getValue())),
                HttpStatus.CONFLICT);
    }
}
