package com.sequenceiq.cloudbreak.service.stack.flow;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.CLUSTER_CB_CM_ADMIN_PASSWORD;
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

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.projection.StackIdView;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
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

    @Test
    public void testRotateSecrets() {
        Stack stack = new Stack();
        stack.setId(1L);
        when(entitlementService.isSecretRotationEnabled(anyString())).thenReturn(Boolean.TRUE);
        when(stackDtoService.getStackViewByCrn(anyString())).thenReturn(stack);
        when(flowManager.triggerSecretRotation(anyLong(), anyString(), any(), any())).thenReturn(new FlowIdentifier(FlowType.FLOW_CHAIN, "flowchain"));

        underTest.rotateSecrets(CRN, List.of(CLUSTER_CB_CM_ADMIN_PASSWORD.name()), null);

        verify(flowManager).triggerSecretRotation(anyLong(), anyString(), any(), any());
    }

    @Test
    public void testRotateSecretsWhenNotEntitled() {
        when(entitlementService.isSecretRotationEnabled(anyString())).thenReturn(Boolean.FALSE);

        assertThrows(CloudbreakServiceException.class, () -> underTest.rotateSecrets(CRN,
                List.of(CLUSTER_CB_CM_ADMIN_PASSWORD.name()), null));

        verifyNoInteractions(flowManager, stackDtoService);
    }

    @Test
    public void testMultiRotateSecrets() {
        Stack stack = new Stack();
        stack.setId(1L);
        when(entitlementService.isSecretRotationEnabled(anyString())).thenReturn(Boolean.TRUE);
        when(stackDtoService.getStackViewByCrn(anyString())).thenReturn(stack);
        when(flowManager.triggerSecretRotation(anyLong(), anyString(), any(), any())).thenReturn(new FlowIdentifier(FlowType.FLOW_CHAIN, "flowchain"));

        underTest.rotateMultiSecrets(CRN, "DEMO_MULTI_SECRET");

        verify(flowManager).triggerSecretRotation(any(), eq(CRN), eq(List.of(CloudbreakSecretType.DATAHUB_DEMO_SECRET)), any());
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
