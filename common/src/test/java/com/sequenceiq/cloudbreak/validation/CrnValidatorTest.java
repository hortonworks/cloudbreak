package com.sequenceiq.cloudbreak.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

import java.lang.annotation.Annotation;

import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.CrnResourceDescriptor;

@ExtendWith(MockitoExtension.class)
public class CrnValidatorTest {

    private static final String MIXED_CRN = "crn:cdp:iam:us-west-1:acc:datalake:res";

    private static final String DATAHUB_CRN = Crn.builder(CrnResourceDescriptor.DATAHUB).setAccountId("acc").setResource("res").build().toString();

    private static final String ENVIRONMENT_CRN = Crn.builder(CrnResourceDescriptor.ENVIRONMENT).setAccountId("acc").setResource("res").build().toString();

    @Captor
    private ArgumentCaptor<String> errorMessageCaptor;

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder builder;

    @InjectMocks
    private CrnValidator underTest;

    @Test
    public void testValidationIfCrnEmpty() {
        assertTrue(underTest.isValid("", context));
    }

    @Test
    public void testValidationIfCrnIsInvalid() {
        setupContext();
        assertFalse(underTest.isValid("invalidcrn", context));
        assertEquals("Invalid crn provided: invalidcrn", errorMessageCaptor.getValue());
    }

    @Test
    public void testValidationIfDescriptorsIsEmpty() {
        underTest.initialize(sampleAnnotation(new CrnResourceDescriptor[]{}));
        assertTrue(underTest.isValid(MIXED_CRN, context));
    }

    @Test
    public void testValidationIfDescriptorProvidedButCrnDoesntMatch() {
        setupContext();
        underTest.initialize(sampleAnnotation(new CrnResourceDescriptor[]{ CrnResourceDescriptor.ENVIRONMENT, CrnResourceDescriptor.DATALAKE }));
        assertFalse(underTest.isValid(MIXED_CRN, context));
        assertEquals("Crn provided: crn:cdp:iam:us-west-1:acc:datalake:res has invalid resource type or service type. " +
                "Accepted service type / resource type pairs: (datalake,datalake),(environments,environment)", errorMessageCaptor.getValue());
        assertFalse(underTest.isValid(DATAHUB_CRN, context));
        assertEquals("Crn provided: crn:cdp:datahub:us-west-1:acc:cluster:res has invalid resource type or service type. " +
                "Accepted service type / resource type pairs: (datalake,datalake),(environments,environment)", errorMessageCaptor.getValue());
    }

    @Test
    public void testValidationIfDescriptorProvidedAndCrnMatches() {
        underTest.initialize(sampleAnnotation(new CrnResourceDescriptor[]{ CrnResourceDescriptor.ENVIRONMENT, CrnResourceDescriptor.DATALAKE }));
        assertTrue(underTest.isValid(ENVIRONMENT_CRN, context));
    }

    private void setupContext() {
        doNothing().when(context).disableDefaultConstraintViolation();
        doReturn(builder).when(context).buildConstraintViolationWithTemplate(errorMessageCaptor.capture());
        doReturn(context).when(builder).addConstraintViolation();
    }

    private ValidCrn sampleAnnotation(CrnResourceDescriptor[] descriptors) {
        return new ValidCrn() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return ValidCrn.class;
            }

            @Override
            public String message() {
                return "Invalid crn provided";
            }

            @Override
            public CrnResourceDescriptor[] resource() {
                return descriptors;
            }

            @Override
            public Class<?>[] groups() {
                return new Class[]{};
            }

            @Override
            public Class<? extends Payload>[] payload() {
                return new Class[]{};
            }
        };
    }
}
