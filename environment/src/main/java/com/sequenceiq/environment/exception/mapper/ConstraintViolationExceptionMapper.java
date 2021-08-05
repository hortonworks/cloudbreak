package com.sequenceiq.environment.exception.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConstraintViolationExceptionMapper.class);

    public ConstraintViolationExceptionMapper() {

    }

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        LOGGER.info(exception.getMessage());
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(prepareMessage(exception))
                .type("application/json")
                .build();
    }

    private ConstraintViolationResponse prepareMessage(ConstraintViolationException exception) {
        List<String> violations = exception.getConstraintViolations().stream()
                .map(cv -> cv.getPropertyPath() + " " + cv.getMessage() + "\n").collect(Collectors.toList());
        return new ConstraintViolationResponse(violations);
    }

    private static class ConstraintViolationResponse {

        private final List<String> constraintViolations;

        ConstraintViolationResponse() {
            constraintViolations = new ArrayList<>();
        }

        ConstraintViolationResponse(List<String> constraintViolations) {
            this.constraintViolations = constraintViolations;
        }

        public List<String> getConstraintViolations() {
            return constraintViolations;
        }
    }
}