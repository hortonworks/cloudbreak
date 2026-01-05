package com.sequenceiq.freeipa.flow.chain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Queue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.model.Entitlement;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.rotation.flow.rotation.event.SecretRotationTriggerEvent;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.freeipa.flow.stack.dynamicentitlement.RefreshEntitlementParamsFlowChainTriggerEvent;
import com.sequenceiq.freeipa.rotation.FreeIpaSecretType;

@ExtendWith(MockitoExtension.class)
class RefreshEntitlementParamsFlowEventChainFactoryTest {

    private static final Long STACK_ID = 1L;

    private static final String ACCOUNT_ID = "account1";

    private static final String RESOURCE_CRN = "crn:cdp:freeipa:us-west-1:" + ACCOUNT_ID + ":stack:resource1";

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private RefreshEntitlementParamsFlowEventChainFactory underTest;

    @Test
    void testWhenTunnelIsCCMV2JumpgateAndEntitlementsAreEnabled() {
        RefreshEntitlementParamsFlowChainTriggerEvent event = new RefreshEntitlementParamsFlowChainTriggerEvent(
                FlowChainTriggers.REFRESH_ENTITLEMENT_PARAM_CHAIN_TRIGGER_EVENT, "opId", STACK_ID, RESOURCE_CRN,
                Map.of(Entitlement.CDP_JUMPGATE_ROOT_CA_AUTO_ROTATION.name(), true), false, Tunnel.CCMV2_JUMPGATE);

        when(entitlementService.isJumpgateRootCertAutoRotationEnabled(ACCOUNT_ID)).thenReturn(true);
        when(entitlementService.isJumpgateNewRootCertEnabled(ACCOUNT_ID)).thenReturn(true);

        FlowTriggerEventQueue result = underTest.createFlowTriggerEventQueue(event);

        Queue<Selectable> queue = result.getQueue();
        SecretRotationTriggerEvent rotationEvent = findEvent(queue, SecretRotationTriggerEvent.class);

        assertNotNull(rotationEvent, "SecretRotationTriggerEvent should be present in the chain");
        assertEquals(FreeIpaSecretType.CCMV2_JUMPGATE_AGENT_ACCESS_KEY, rotationEvent.getSecretType());
        assertEquals(STACK_ID, rotationEvent.getResourceId());
    }

    @Test
    void testWhenEntitlementsAreNotChanged() {
        RefreshEntitlementParamsFlowChainTriggerEvent event = new RefreshEntitlementParamsFlowChainTriggerEvent(
                FlowChainTriggers.REFRESH_ENTITLEMENT_PARAM_CHAIN_TRIGGER_EVENT, "opId", STACK_ID, RESOURCE_CRN,
                Map.of(), false, Tunnel.CCMV2_JUMPGATE);

        FlowTriggerEventQueue result = underTest.createFlowTriggerEventQueue(event);

        Queue<Selectable> queue = result.getQueue();
        assertNull(findEvent(queue, SecretRotationTriggerEvent.class));
    }

    @Test
    void testWhenNewRootCertEntitlementIsNotEnabled() {
        RefreshEntitlementParamsFlowChainTriggerEvent event = new RefreshEntitlementParamsFlowChainTriggerEvent(
                FlowChainTriggers.REFRESH_ENTITLEMENT_PARAM_CHAIN_TRIGGER_EVENT, "opId", STACK_ID, RESOURCE_CRN,
                Map.of(Entitlement.CDP_JUMPGATE_ROOT_CA_AUTO_ROTATION.name(), true), false, Tunnel.CCMV2_JUMPGATE);

        when(entitlementService.isJumpgateRootCertAutoRotationEnabled(ACCOUNT_ID)).thenReturn(true);
        when(entitlementService.isJumpgateNewRootCertEnabled(ACCOUNT_ID)).thenReturn(false);

        FlowTriggerEventQueue result = underTest.createFlowTriggerEventQueue(event);

        Queue<Selectable> queue = result.getQueue();
        assertNull(findEvent(queue, SecretRotationTriggerEvent.class));
    }

    @Test
    void testWhenTunnelIsNotCCMV2Jumpgate() {
        RefreshEntitlementParamsFlowChainTriggerEvent event = new RefreshEntitlementParamsFlowChainTriggerEvent(
                FlowChainTriggers.REFRESH_ENTITLEMENT_PARAM_CHAIN_TRIGGER_EVENT, "opId", STACK_ID, RESOURCE_CRN,
                Map.of(Entitlement.CDP_JUMPGATE_ROOT_CA_AUTO_ROTATION.name(), true), false, Tunnel.DIRECT);

        when(entitlementService.isJumpgateRootCertAutoRotationEnabled(ACCOUNT_ID)).thenReturn(true);
        when(entitlementService.isJumpgateNewRootCertEnabled(ACCOUNT_ID)).thenReturn(true);

        FlowTriggerEventQueue result = underTest.createFlowTriggerEventQueue(event);

        Queue<Selectable> queue = result.getQueue();
        assertNull(findEvent(queue, SecretRotationTriggerEvent.class));
    }

    @SuppressWarnings("unchecked")
    private <T> T findEvent(Queue<Selectable> queue, Class<T> clazz) {
        return (T) queue.stream()
                .filter(clazz::isInstance)
                .findFirst()
                .orElse(null);
    }
}