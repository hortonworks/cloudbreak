package com.sequenceiq.redbeams.service.rotation;

import static com.sequenceiq.cloudbreak.rotation.secret.RotationFlowExecutionType.ROTATE;
import static com.sequenceiq.cloudbreak.rotation.secret.type.RedbeamsSecretType.REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.flow.rotation.service.SecretRotationValidator;
import com.sequenceiq.redbeams.flow.RedbeamsFlowManager;
import com.sequenceiq.redbeams.service.stack.DBStackService;

@ExtendWith(MockitoExtension.class)
class RedbeamsRotationServiceTest {

    private static final String RESOURCE_CRN = "crn:cdp:redbeams:us-west-1:1234:environment:1";

    private static final Long RESOURCE_ID = 1L;

    @Mock
    private RedbeamsFlowManager redbeamsFlowManager;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private DBStackService dbStackService;

    @Mock
    private SecretRotationValidator secretRotationValidator;

    @InjectMocks
    private RedbeamsRotationService underTest;

    @Test
    void rotateSecretsShouldSucceed() {
        when(secretRotationValidator.mapSecretTypes(anyList(), any())).thenReturn(List.of(REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD));
        when(dbStackService.getResourceIdByResourceCrn(eq(RESOURCE_CRN))).thenReturn(RESOURCE_ID);
        when(entitlementService.isSecretRotationEnabled(anyString())).thenReturn(Boolean.TRUE);
        underTest.rotateSecrets(RESOURCE_CRN, List.of(REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD.name()), ROTATE);
        verify(redbeamsFlowManager, times(1)).triggerSecretRotation(eq(RESOURCE_ID), eq(RESOURCE_CRN),
                eq(List.of(REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD)), eq(ROTATE));
    }

    @Test
    void rotateSecretsShouldFailIfNoResourceIdFound() {
        when(dbStackService.getResourceIdByResourceCrn(eq(RESOURCE_CRN))).thenReturn(null);
        when(entitlementService.isSecretRotationEnabled(anyString())).thenReturn(Boolean.TRUE);
        CloudbreakServiceException cloudbreakServiceException = assertThrows(CloudbreakServiceException.class,
                () -> underTest.rotateSecrets(RESOURCE_CRN, List.of(REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD.name()), ROTATE));
        verify(redbeamsFlowManager, never()).triggerSecretRotation(eq(RESOURCE_ID), eq(RESOURCE_CRN),
                eq(List.of(REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD)), eq(ROTATE));
        assertEquals("No db stack found with crn: " + RESOURCE_CRN, cloudbreakServiceException.getMessage());
    }
}