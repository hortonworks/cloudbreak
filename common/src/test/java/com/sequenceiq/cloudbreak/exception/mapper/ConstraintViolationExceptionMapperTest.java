package com.sequenceiq.cloudbreak.exception.mapper;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.Response;

import org.hibernate.validator.internal.engine.path.PathImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.ExceptionResponse;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;

@ExtendWith(MockitoExtension.class)
public class ConstraintViolationExceptionMapperTest {

    @InjectMocks
    private ConstraintViolationExceptionMapper underTest;

    @Test
    public void testToResponseWhenHasOneValidationError() {
        ConstraintViolation<Object> constraintViolation = mock(ConstraintViolation.class);
        when(constraintViolation.getMessage()).thenReturn("something validation error occurred");
        when(constraintViolation.getPropertyPath()).thenReturn(PathImpl.createPathFromString("path.smgth"));
        ConstraintViolationException exception = new ConstraintViolationException("Error message", Set.of(constraintViolation));
        Response response = underTest.toResponse(exception);
        ExceptionResponse entity = (ExceptionResponse) response.getEntity();
        String actual = JsonUtil.writeValueAsStringSilentSafe(entity);
        String expected = "{\"message\":\"something validation error occurred\"," +
                "\"payload\":[{\"field\":\"path.smgth\",\"result\":\"something validation error occurred\"}]}";
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testToResponseWhenHasTwoValidationError() {
        ConstraintViolation<Object> constraintViolation1 = mock(ConstraintViolation.class);
        ConstraintViolation<Object> constraintViolation2 = mock(ConstraintViolation.class);
        when(constraintViolation1.getMessage()).thenReturn("something validation error occurred");
        when(constraintViolation1.getPropertyPath()).thenReturn(PathImpl.createPathFromString("path.smgth"));
        when(constraintViolation2.getMessage()).thenReturn("other validation error happened");
        ConstraintViolationException exception = new ConstraintViolationException("Error message", Set.of(constraintViolation1, constraintViolation2));
        Response response = underTest.toResponse(exception);
        ExceptionResponse entity = (ExceptionResponse) response.getEntity();
        String actual = JsonUtil.writeValueAsStringSilentSafe(entity);
        String expected = "{\"message\":\"More than one validation errors happened: \\nsomething validation error occurred" +
                "\\nother validation error happened\",\"payload\":[{\"field\":\"path.smgth\",\"result\":\"something validation error occurred\"}," +
                "{\"field\":\"\",\"result\":\"other validation error happened\"}]}";
        Assertions.assertEquals(expected, actual);
    }
}
