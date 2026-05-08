package com.sequenceiq.cloudbreak.core.flow2.cluster.certrenew;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.service.publicendpoint.GatewayPublicEndpointManagementService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.FlowTriggerConditionResult;

@ExtendWith(MockitoExtension.class)
class ClusterCertificateRenewTriggerConditionTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private GatewayPublicEndpointManagementService gatewayPublicEndpointManagementService;

    @InjectMocks
    private ClusterCertificateRenewTriggerCondition underTest;

    @Mock
    private Payload payload;

    @Mock
    private StackView stackView;

    @Mock
    private StackStatus stackStatus;

    @BeforeEach
    void setUp() {
        when(payload.getResourceId()).thenReturn(STACK_ID);
        when(stackDtoService.getStackViewById(STACK_ID)).thenReturn(stackView);
    }

    @Test
    void isFlowTriggerableWhenTriggerable() {
        when(stackView.getClusterId()).thenReturn(1L);
        when(stackView.isAvailable()).thenReturn(true);
        when(gatewayPublicEndpointManagementService.isCertRenewalTriggerable(stackView)).thenReturn(true);

        FlowTriggerConditionResult result = underTest.isFlowTriggerable(payload);

        assertTrue(result.isOk());
    }

    @Test
    void isFlowTriggerableWhenNotAvailable() {
        when(stackView.getClusterId()).thenReturn(1L);
        when(stackView.isAvailable()).thenReturn(false);
        when(stackView.hasNodeFailure()).thenReturn(false);
        when(stackView.getStackStatus()).thenReturn(stackStatus);
        when(stackStatus.getDetailedStackStatus()).thenReturn(DetailedStackStatus.UNKNOWN);
        when(gatewayPublicEndpointManagementService.isCertRenewalTriggerable(stackView)).thenReturn(true);

        FlowTriggerConditionResult result = underTest.isFlowTriggerable(payload);

        assertTrue(result.isFail());
        assertTrue(result.getErrorMessage().contains("not available"));
    }

    @Test
    void isFlowTriggerableWhenNoCluster() {
        when(stackView.getClusterId()).thenReturn(null);
        when(stackView.getStackStatus()).thenReturn(stackStatus);
        when(stackStatus.getDetailedStackStatus()).thenReturn(DetailedStackStatus.UNKNOWN);
        when(gatewayPublicEndpointManagementService.isCertRenewalTriggerable(stackView)).thenReturn(true);

        FlowTriggerConditionResult result = underTest.isFlowTriggerable(payload);

        assertTrue(result.isFail());
        assertTrue(result.getErrorMessage().contains("not available"));
    }

    @Test
    void isFlowTriggerableWhenCertRenewalNotTriggerable() {
        when(stackView.getClusterId()).thenReturn(1L);
        when(stackView.isAvailable()).thenReturn(true);
        when(gatewayPublicEndpointManagementService.isCertRenewalTriggerable(stackView)).thenReturn(false);

        FlowTriggerConditionResult result = underTest.isFlowTriggerable(payload);

        assertTrue(result.isFail());
        assertTrue(result.getErrorMessage().contains("generation feature is disabled"));
    }

    @Test
    void isFlowTriggerableWhenHasNodeFailure() {
        when(stackView.getClusterId()).thenReturn(1L);
        when(stackView.isAvailable()).thenReturn(false);
        when(stackView.hasNodeFailure()).thenReturn(true);
        when(gatewayPublicEndpointManagementService.isCertRenewalTriggerable(stackView)).thenReturn(true);

        FlowTriggerConditionResult result = underTest.isFlowTriggerable(payload);

        assertTrue(result.isOk());
    }

    @Test
    void isFlowTriggerableWhenCertRenewalFailed() {
        when(stackView.getClusterId()).thenReturn(1L);
        when(stackView.isAvailable()).thenReturn(false);
        when(stackView.hasNodeFailure()).thenReturn(false);
        when(stackView.getStackStatus()).thenReturn(stackStatus);
        when(stackStatus.getDetailedStackStatus()).thenReturn(DetailedStackStatus.CERTIFICATE_RENEWAL_FAILED);
        when(gatewayPublicEndpointManagementService.isCertRenewalTriggerable(stackView)).thenReturn(true);

        FlowTriggerConditionResult result = underTest.isFlowTriggerable(payload);

        assertTrue(result.isOk());
    }
}