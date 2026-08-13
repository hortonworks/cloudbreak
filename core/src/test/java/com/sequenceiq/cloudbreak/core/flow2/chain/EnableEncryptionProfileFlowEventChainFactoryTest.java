package com.sequenceiq.cloudbreak.core.flow2.chain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.core.flow2.cluster.config.update.PillarConfigurationUpdateEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.EnableEncryptionProfileOnClusterStateSelectors;
import com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.event.EnableEncryptionProfileOnClusterEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.restartcm.RestartClusterManagerFlowEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.EnableEncryptionProfileTriggerEvent;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@ExtendWith(MockitoExtension.class)
class EnableEncryptionProfileFlowEventChainFactoryTest {

    private static final long STACK_ID = 1L;

    private static final String ENCRYPTION_PROFILE_CRN = "crn:cdp:environments:us-west-1:tenant:encryptionProfile:custom-123";

    private static final EnableEncryptionProfileTriggerEvent EVENT = new EnableEncryptionProfileTriggerEvent(
            FlowChainTriggers.ENABLE_ENCRYPTION_PROFILE_CHAIN_TRIGGER_EVENT, STACK_ID, new Promise<>(), ENCRYPTION_PROFILE_CRN);

    @InjectMocks
    private EnableEncryptionProfileFlowEventChainFactory underTest;

    @Test
    void testInitEventShouldReturnEnableEncryptionProfileChainTriggerEvent() {
        assertThat(underTest.initEvent()).isEqualTo(FlowChainTriggers.ENABLE_ENCRYPTION_PROFILE_CHAIN_TRIGGER_EVENT);
    }

    @Test
    void testCreateFlowTriggerEventQueueEmitsExpectedOrderAndPropagatesEncryptionProfileCrn() {
        FlowTriggerEventQueue result = underTest.createFlowTriggerEventQueue(EVENT);

        assertThat(result).returns(EVENT, FlowTriggerEventQueue::getTriggerEvent);

        Object first = result.getQueue().poll();
        assertThat(first).isInstanceOf(EnableEncryptionProfileOnClusterEvent.class);
        EnableEncryptionProfileOnClusterEvent enableEvent = (EnableEncryptionProfileOnClusterEvent) first;
        assertThat(enableEvent.selector())
                .isEqualTo(EnableEncryptionProfileOnClusterStateSelectors.ENABLE_ENCRYPTION_PROFILE_ON_CLUSTER_EVENT.event());
        assertThat(enableEvent.getResourceId()).isEqualTo(STACK_ID);
        assertThat(enableEvent.getEncryptionProfileCrn()).isEqualTo(ENCRYPTION_PROFILE_CRN);
        assertThat(result.getQueue().poll())
                .isInstanceOf(StackEvent.class)
                .extracting(StackEvent.class::cast)
                .returns(PillarConfigurationUpdateEvent.PILLAR_CONFIG_UPDATE_EVENT.event(), StackEvent::selector)
                .returns(STACK_ID, StackEvent::getResourceId);
        assertThat(result.getQueue().poll())
                .isInstanceOf(StackEvent.class)
                .extracting(StackEvent.class::cast)
                .returns(RestartClusterManagerFlowEvent.RESTART_CLUSTER_MANAGER_TRIGGER_EVENT.event(), StackEvent::selector)
                .returns(STACK_ID, StackEvent::getResourceId);

        assertThat(result.getQueue()).isEmpty();
    }
}