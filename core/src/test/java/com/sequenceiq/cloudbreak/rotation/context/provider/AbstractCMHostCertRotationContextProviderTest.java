package com.sequenceiq.cloudbreak.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CUSTOM_JOB;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.rotation.RotationNodeValidationService;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;

@ExtendWith(MockitoExtension.class)
public class AbstractCMHostCertRotationContextProviderTest {

    private static final String RESOURCE_CRN = "crn";

    @Mock
    private RotationNodeValidationService rotationNodeValidationService;

    @InjectMocks
    private CMIntermediateCacertRotationContextProvider underTest;

    @Test
    void testGetContexts() {
        Map<SecretRotationStep, RotationContext> contexts = underTest.getContexts(RESOURCE_CRN);

        assertEquals(1, contexts.size());
        assertTrue(contexts.containsKey(CUSTOM_JOB));
    }

    @Test
    void testPreValidateSucceedsWhenNoStoppedInstances() {
        doNothing().when(rotationNodeValidationService).validateNoStoppedInstances(eq(RESOURCE_CRN), any(SecretType.class));

        CustomJobRotationContext context = (CustomJobRotationContext) underTest.getContexts(RESOURCE_CRN).get(CUSTOM_JOB);

        assertDoesNotThrow(() -> context.getPreValidateJob().ifPresent(Runnable::run));
    }

    @Test
    void testPreValidateFailsWhenStoppedInstancesExist() {
        doThrow(new SecretRotationException("There are stopped instances in the cluster, " +
                "'CM_INTERMEDIATE_CA_CERT' rotation cannot be performed. " +
                "Please start all stopped nodes before retrying. Stopped instances: [host1.example.com]"))
                .when(rotationNodeValidationService).validateNoStoppedInstances(eq(RESOURCE_CRN), any(SecretType.class));

        CustomJobRotationContext context = (CustomJobRotationContext) underTest.getContexts(RESOURCE_CRN).get(CUSTOM_JOB);

        SecretRotationException exception = assertThrows(SecretRotationException.class,
                () -> context.getPreValidateJob().ifPresent(Runnable::run));
        assertTrue(exception.getMessage().contains("stopped instances"));
        assertTrue(exception.getMessage().contains("host1.example.com"));
    }
}
