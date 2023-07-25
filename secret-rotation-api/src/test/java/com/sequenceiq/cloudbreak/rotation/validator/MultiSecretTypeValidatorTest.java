package com.sequenceiq.cloudbreak.rotation.validator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;

import java.lang.annotation.Annotation;

import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.rotation.MultiSecretType;
import com.sequenceiq.cloudbreak.rotation.annotation.ValidMultiSecretType;
import com.sequenceiq.cloudbreak.rotation.common.TestMultiSecretType;

@ExtendWith(MockitoExtension.class)
public class MultiSecretTypeValidatorTest {

    @Captor
    private ArgumentCaptor<String> errorMessageCaptor;

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder builder;

    @BeforeEach
    void setup() {
        setupContext();
    }

    @Test
    void testValidation() {
        assertTrue(validator().isValid("MULTI_TEST", context));
        assertFalse(validator().isValid("TEST", context));
    }

    private MultiSecretTypeValidator validator() {
        MultiSecretTypeValidator validator = new MultiSecretTypeValidator();
        validator.initialize(new ValidMultiSecretType() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return null;
            }

            @Override
            public Class<? extends MultiSecretType>[] allowedTypes() {
                return new Class[] { TestMultiSecretType.class };
            }

            @Override
            public String message() {
                return null;
            }

            @Override
            public Class<?>[] groups() {
                return new Class[0];
            }

            @Override
            public Class<? extends Payload>[] payload() {
                return new Class[0];
            }
        });
        return validator;
    }

    private void setupContext() {
        lenient().doReturn(builder).when(context).buildConstraintViolationWithTemplate(errorMessageCaptor.capture());
        lenient().doReturn(context).when(builder).addConstraintViolation();
    }
}
