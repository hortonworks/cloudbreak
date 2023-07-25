package com.sequenceiq.cloudbreak.rotation.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;

import java.lang.annotation.Annotation;
import java.util.List;

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
import com.sequenceiq.cloudbreak.rotation.annotation.ValidSecretTypes;
import com.sequenceiq.cloudbreak.rotation.common.TestSecretType;

@ExtendWith(MockitoExtension.class)
public class SecretTypesValidatorTest {

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
        assertFalse(validator(false, true).isValid(List.of("INVALID"), context));
        assertEquals("Invalid secret type, cannot map secrets [INVALID].", errorMessageCaptor.getValue());
    }

    @Test
    void testValidateIfOnlyInternalAllowed() {
        assertFalse(validator(true, true).isValid(List.of("TEST"), context));
        assertEquals("Only internal secret type is allowed!", errorMessageCaptor.getValue());
    }

    @Test
    void testValidateIfMultiNotAllowed() {
        assertFalse(validator(false, false).isValid(List.of("TEST_2"), context));
        assertEquals("Only single secret type is allowed!", errorMessageCaptor.getValue());
    }

    @Test
    void testValidateIfMDuplicated() {
        assertFalse(validator(false, true).isValid(List.of("TEST_2", "TEST_2"), context));
        assertEquals("There is at least one duplication in the request!", errorMessageCaptor.getValue());
    }

    @Test
    void testValidate() {
        assertTrue(validator(false, true).isValid(List.of("TEST_3"), context));
    }

    private SecretTypesValidator validator(boolean internalOnlyAllowed, boolean multiAllowed) {
        SecretTypesValidator validator = new SecretTypesValidator();
        validator.initialize(new ValidSecretTypes() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return null;
            }

            @Override
            public Class<? extends SecretType>[] allowedTypes() {
                return new Class[] { TestSecretType.class };
            }

            @Override
            public boolean internalOnlyAllowed() {
                return internalOnlyAllowed;
            }

            @Override
            public boolean multiAllowed() {
                return multiAllowed;
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
