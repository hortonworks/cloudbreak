package com.sequenceiq.cloudbreak.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

import java.lang.annotation.Annotation;
import java.util.Set;

import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.base.Joiner;
import com.sequenceiq.cloudbreak.auth.CrnTestUtil;
import com.sequenceiq.cloudbreak.auth.altus.CrnResourceDescriptor;

@ExtendWith(MockitoExtension.class)
public class CrnValidatorTest {

    private static final String MIXED_CRN = "crn:cdp:iam:us-west-1:acc:datalake:res";

    private static final String DATAHUB_CRN = CrnTestUtil.getDatahubCrnBuilder().setAccountId("acc").setResource("res").build().toString();

    private static final String ENVIRONMENT_CRN = CrnTestUtil.getEnvironmentCrnBuilder().setAccountId("acc").setResource("res").build().toString();

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
        underTest.initialize(sampleAnnotation(new CrnResourceDescriptor[]{}, ValidCrn.Effect.ACCEPT));
        assertTrue(underTest.isValid(MIXED_CRN, context));
    }

    @ParameterizedTest
    @EnumSource(ValidCrn.Effect.class)
    public void testGetErrorMessageIfServiceOrResourceTypeInvalid(ValidCrn.Effect effect) {

        Set<Pair> serviceAndResourceTypePairs = Set.of(Pair.of("myService", "myResource"));
        String message = underTest.getErrorMessageIfServiceOrResourceTypeInvalid("myCrn", serviceAndResourceTypePairs, effect);

        String expectedMessage = String.format("Crn provided: myCrn has invalid resource type or service type. %s service type / resource type pairs: %s",
                effect.getName(), Joiner.on(",").join(serviceAndResourceTypePairs));
        assertEquals(expectedMessage, message);
    }

    @Test
    public void testValidationIfDescriptorProvidedButCrnDoesntMatchWhenEffectAccept() {
        setupContext();
        underTest.initialize(
                sampleAnnotation(new CrnResourceDescriptor[]{ CrnResourceDescriptor.ENVIRONMENT, CrnResourceDescriptor.DATALAKE }, ValidCrn.Effect.ACCEPT));
        assertFalse(underTest.isValid(MIXED_CRN, context));
        assertEquals("Crn provided: crn:cdp:iam:us-west-1:acc:datalake:res has invalid resource type or service type. " +
                "Accepted service type / resource type pairs: (datalake,datalake),(environments,environment)", errorMessageCaptor.getValue());
        assertFalse(underTest.isValid(DATAHUB_CRN, context));
        assertEquals("Crn provided: crn:cdp:datahub:us-west-1:acc:cluster:res has invalid resource type or service type. " +
                "Accepted service type / resource type pairs: (datalake,datalake),(environments,environment)", errorMessageCaptor.getValue());
    }

    @Test
    public void testValidationIfDescriptorProvidedButCrnMatchesWhenEffectDeny() {
        setupContext();
        underTest.initialize(
                sampleAnnotation(new CrnResourceDescriptor[]{ CrnResourceDescriptor.ENVIRONMENT, CrnResourceDescriptor.DATALAKE }, ValidCrn.Effect.DENY));
        assertFalse(underTest.isValid(ENVIRONMENT_CRN, context));
        assertEquals("Crn provided: crn:cdp:environments:us-west-1:acc:environment:res has invalid resource type or service type. Denied service type" +
                " / resource type pairs: (datalake,datalake),(environments,environment)", errorMessageCaptor.getValue());
    }

    @Test
    public void testValidationIfDescriptorProvidedAndCrnMatchesWhenEffectAccept() {
        underTest.initialize(
                sampleAnnotation(new CrnResourceDescriptor[]{ CrnResourceDescriptor.ENVIRONMENT, CrnResourceDescriptor.DATALAKE }, ValidCrn.Effect.ACCEPT));
        assertTrue(underTest.isValid(ENVIRONMENT_CRN, context));
    }

    @Test
    public void testValidationIfDescriptorProvidedAndCrnMatchesWhenEffectDeny() {
        underTest.initialize(
                sampleAnnotation(new CrnResourceDescriptor[]{ CrnResourceDescriptor.ENVIRONMENT, CrnResourceDescriptor.DATALAKE }, ValidCrn.Effect.DENY));
        assertTrue(underTest.isValid(MIXED_CRN, context));
    }

    private void setupContext() {
        doNothing().when(context).disableDefaultConstraintViolation();
        doReturn(builder).when(context).buildConstraintViolationWithTemplate(errorMessageCaptor.capture());
        doReturn(context).when(builder).addConstraintViolation();
    }

    private ValidCrn sampleAnnotation(CrnResourceDescriptor[] descriptors, ValidCrn.Effect effect) {
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
            public Effect effect() {
                return effect;
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
