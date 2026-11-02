package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.ENABLE_ENCRYPTION_PROFILE_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.ENABLE_ENCRYPTION_PROFILE_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.ENABLE_ENCRYPTION_PROFILE_STARTED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UNSET;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value;
import com.sequenceiq.cloudbreak.core.flow2.cluster.config.update.PillarConfigUpdateState;
import com.sequenceiq.cloudbreak.core.flow2.cluster.config.update.PillarConfigurationUpdateEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.EnableEncryptionProfileOnClusterState;
import com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.EnableEncryptionProfileOnClusterStateSelectors;
import com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.event.EnableEncryptionProfileOnClusterEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.restartcm.RestartClusterManagerFlowEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.restartcm.RestartClusterManagerFlowState;
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

    @Test
    void testCreateFlowTriggerEventQueuePropagatesTriggerAcceptPromiseToEveryElement() {
        EnableEncryptionProfileTriggerEvent triggerEvent = new EnableEncryptionProfileTriggerEvent(
                FlowChainTriggers.ENABLE_ENCRYPTION_PROFILE_CHAIN_TRIGGER_EVENT, STACK_ID, new Promise<>(), ENCRYPTION_PROFILE_CRN);

        FlowTriggerEventQueue result = underTest.createFlowTriggerEventQueue(triggerEvent);

        assertThat(result.getQueue())
                .isNotEmpty()
                .allSatisfy(selectable -> assertThat(((StackEvent) selectable).accepted()).isSameAs(triggerEvent.accepted()));
    }

    @ParameterizedTest(name = "Use case for enable encryption profile on cluster flow state: {0}")
    @EnumSource(EnableEncryptionProfileOnClusterState.class)
    void testGetUseCaseForEnableEncryptionProfileOnClusterState(EnableEncryptionProfileOnClusterState flowState) {
        Value expected = switch (flowState) {
            case INIT_STATE -> ENABLE_ENCRYPTION_PROFILE_STARTED;
            case ENABLE_ENCRYPTION_PROFILE_ON_CLUSTER_FAILED_STATE -> ENABLE_ENCRYPTION_PROFILE_FAILED;
            default -> UNSET;
        };
        assertThat(underTest.getUseCaseForFlowState(flowState)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "Use case for pillar config update flow state: {0}")
    @EnumSource(PillarConfigUpdateState.class)
    void testGetUseCaseForPillarConfigUpdateState(PillarConfigUpdateState flowState) {
        Value expected = PillarConfigUpdateState.PILLAR_CONFIG_UPDATE_FAILED_STATE == flowState ? ENABLE_ENCRYPTION_PROFILE_FAILED : UNSET;
        assertThat(underTest.getUseCaseForFlowState(flowState)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "Use case for restart cluster manager flow state: {0}")
    @EnumSource(RestartClusterManagerFlowState.class)
    void testGetUseCaseForRestartClusterManagerFlowState(RestartClusterManagerFlowState flowState) {
        Value expected = switch (flowState) {
            case RESTART_CLUSTER_MANAGER_FINISED_STATE -> ENABLE_ENCRYPTION_PROFILE_FINISHED;
            case RESTART_CLUSTER_MANAGER_FAILED_STATE -> ENABLE_ENCRYPTION_PROFILE_FAILED;
            default -> UNSET;
        };
        assertThat(underTest.getUseCaseForFlowState(flowState)).isEqualTo(expected);
    }
}
