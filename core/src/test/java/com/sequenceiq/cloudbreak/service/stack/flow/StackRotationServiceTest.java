package com.sequenceiq.cloudbreak.service.stack.flow;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.CLUSTER_CB_CM_ADMIN_PASSWORD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.projection.StackIdView;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.rotation.service.SecretRotationValidationService;
import com.sequenceiq.cloudbreak.rotation.service.multicluster.MultiClusterRotationService;
import com.sequenceiq.cloudbreak.rotation.service.multicluster.MultiClusterRotationValidationService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;

@ExtendWith(MockitoExtension.class)
public class StackRotationServiceTest {

    private static final String CRN = "crn:cdp:datahub:us-west-1:tenant:cluster:878605d9-f9e9-44c6-9da6-e4bce9570ef5";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:9d74eee4:environment:12474ddc";

    private static final String DATALAKE_CRN = "crn:cdp:datalake:us-west-1:default:datalake:d3b8df82-878d-4395-94b1-2e355217446d";

    @InjectMocks
    private StackRotationService underTest;

    @Mock
    private ReactorFlowManager flowManager;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private StackService stackService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private MultiClusterRotationValidationService multiClusterRotationValidationService;

    @Mock
    private MultiClusterRotationService multiClusterRotationService;

    @Mock
    private SecretRotationValidationService secretRotationValidationService;

    @Test
    public void testRotateSecrets() {
        Stack stack = new Stack();
        stack.setId(1L);
        stack.setStackStatus(new StackStatus(null, DetailedStackStatus.AVAILABLE));
        doNothing().when(secretRotationValidationService).validateExecutionType(any(), any(), any());
        when(entitlementService.isSecretRotationEnabled(anyString())).thenReturn(Boolean.TRUE);
        when(stackDtoService.getStackViewByCrn(anyString())).thenReturn(stack);
        when(flowManager.triggerSecretRotation(anyLong(), anyString(), any(), any(), any())).thenReturn(new FlowIdentifier(FlowType.FLOW_CHAIN, "flowchain"));

        underTest.rotateSecrets(CRN, List.of(CLUSTER_CB_CM_ADMIN_PASSWORD.name()), null, null);

        verify(flowManager).triggerSecretRotation(anyLong(), anyString(), any(), any(), any());
    }

    @Test
    public void testRotateSecretsWhenClusterIsNotAvailable() {
        Stack stack = new Stack();
        stack.setId(1L);
        stack.setStackStatus(new StackStatus(null, DetailedStackStatus.STOPPED));
        when(entitlementService.isSecretRotationEnabled(anyString())).thenReturn(Boolean.TRUE);
        when(stackDtoService.getStackViewByCrn(anyString())).thenReturn(stack);

        CloudbreakServiceException exception =
                assertThrows(CloudbreakServiceException.class, () -> underTest.rotateSecrets(CRN, List.of(CLUSTER_CB_CM_ADMIN_PASSWORD.name()), null, null));
        assertEquals("The cluster must be in available status to execute secret rotation. Current status: STOPPED", exception.getMessage());
    }

    @Test
    public void testRotateSecretsWhenNotEntitled() {
        when(entitlementService.isSecretRotationEnabled(anyString())).thenReturn(Boolean.FALSE);

        assertThrows(CloudbreakServiceException.class, () -> underTest.rotateSecrets(CRN,
                List.of(CLUSTER_CB_CM_ADMIN_PASSWORD.name()), null, null));

        verifyNoInteractions(flowManager, stackDtoService);
    }

    @Test
    public void testMarkMultiClusterChildrenResourceByEnv() {
        when(stackService.getByEnvironmentCrnAndStackType(any(), any())).thenReturn(List.of(getStackIdView()));
        doNothing().when(multiClusterRotationService).markChildrenMultiRotationEntriesLocally(any(), any());

        underTest.markMultiClusterChildrenResources(ENV_CRN, "DEMO_MULTI_SECRET");

        verify(stackService).getByEnvironmentCrnAndStackType(eq(ENV_CRN), any());
        verify(stackService, times(0)).findByDatalakeCrn(any());
    }

    @Test
    public void testMarkMultiClusterChildrenResourceByDL() {
        when(stackService.findByDatalakeCrn(any())).thenReturn(Set.of(getStackIdView()));
        doNothing().when(multiClusterRotationService).markChildrenMultiRotationEntriesLocally(any(), any());

        underTest.markMultiClusterChildrenResources(DATALAKE_CRN, "DEMO_MULTI_SECRET");

        verify(stackService, times(0)).getByEnvironmentCrnAndStackType(any(), any());
        verify(stackService).findByDatalakeCrn(eq(DATALAKE_CRN));
    }

    private static StackIdView getStackIdView() {
        return new StackIdView() {

            @Override
            public Long getId() {
                return null;
            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public String getCrn() {
                return CRN;
            }
        };
    }
}
