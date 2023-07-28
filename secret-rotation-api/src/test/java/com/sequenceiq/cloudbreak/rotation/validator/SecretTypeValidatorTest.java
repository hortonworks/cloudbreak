package com.sequenceiq.cloudbreak.rotation.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.annotation.ValidSecretType;
import com.sequenceiq.cloudbreak.rotation.common.TestSecretType;

@ExtendWith(MockitoExtension.class)
public class SecretTypeValidatorTest {

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
    void testValidateIfInvalidType() {
        assertFalse(validator(false).isValid("INVALID", context));
        assertEquals("Invalid secret type, cannot map secret INVALID.", errorMessageCaptor.getValue());
    }

    @Test
    void testValidateIfInternalNotAllowed() {
        assertFalse(validator(false).isValid("TEST_3", context));
    }

    @Test
    void testValidate() {
        assertTrue(validator(true).isValid("TEST_3", context));
    }

    private SecretTypeValidator validator(boolean internalAllowed) {
        SecretTypeValidator validator = new SecretTypeValidator();
        validator.initialize(new ValidSecretType() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return null;
            }

            @Override
            public Class<? extends SecretType>[] allowedTypes() {
                return new Class[] { TestSecretType.class };
            }

            @Override
            public boolean internalAllowed() {
                return internalAllowed;
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
