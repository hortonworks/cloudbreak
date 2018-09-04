package com.sequenceiq.cloudbreak.validation;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.validation.ConstraintValidatorContext;

public class ContextMockUtil {

    private ContextMockUtil() {

    }

    public static ConstraintValidatorContext createContextMock() {
        ConstraintValidatorContext contextMock = mock(ConstraintValidatorContext.class);
        ConstraintValidatorContext.ConstraintViolationBuilder constraintViolationBuilderMock = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext nodeBuilderContextMock
                = mock(ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext.class);

        when(contextMock.buildConstraintViolationWithTemplate(anyString()))
                .thenReturn(constraintViolationBuilderMock);
        when(constraintViolationBuilderMock.addPropertyNode(anyString()))
                .thenReturn(nodeBuilderContextMock);
        when(nodeBuilderContextMock.addConstraintViolation()).thenReturn(contextMock);
        return contextMock;
    }

}
