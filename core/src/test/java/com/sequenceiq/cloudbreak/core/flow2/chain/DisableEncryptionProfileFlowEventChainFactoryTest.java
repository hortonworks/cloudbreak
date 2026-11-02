package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.DISABLE_ENCRYPTION_PROFILE_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.DISABLE_ENCRYPTION_PROFILE_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.DISABLE_ENCRYPTION_PROFILE_STARTED;
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
import com.sequenceiq.cloudbreak.core.flow2.cluster.restartcm.RestartClusterManagerFlowEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.restartcm.RestartClusterManagerFlowState;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@ExtendWith(MockitoExtension.class)
class DisableEncryptionProfileFlowEventChainFactoryTest {

    private static final long STACK_ID = 1L;

    private static final StackEvent EVENT =
            new StackEvent(FlowChainTriggers.DISABLE_ENCRYPTION_PROFILE_CHAIN_TRIGGER_EVENT, STACK_ID, new Promise<>());

    @InjectMocks
    private DisableEncryptionProfileFlowEventChainFactory underTest;

    @Test
    void testInitEventShouldReturnDisableEncryptionProfileChainTriggerEvent() {
        assertThat(underTest.initEvent()).isEqualTo(FlowChainTriggers.DISABLE_ENCRYPTION_PROFILE_CHAIN_TRIGGER_EVENT);
    }

    @Test
    void testCreateFlowTriggerEventQueue() {
        FlowTriggerEventQueue result = underTest.createFlowTriggerEventQueue(EVENT);

        assertThat(result)
                .returns(EVENT, FlowTriggerEventQueue::getTriggerEvent);

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

    @ParameterizedTest(name = "Use case for pillar config update flow state: {0}")
    @EnumSource(PillarConfigUpdateState.class)
    void testGetUseCaseForPillarConfigUpdateState(PillarConfigUpdateState flowState) {
        Value expected = switch (flowState) {
            case INIT_STATE -> DISABLE_ENCRYPTION_PROFILE_STARTED;
            case PILLAR_CONFIG_UPDATE_FAILED_STATE -> DISABLE_ENCRYPTION_PROFILE_FAILED;
            default -> UNSET;
        };
        assertThat(underTest.getUseCaseForFlowState(flowState)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "Use case for restart cluster manager flow state: {0}")
    @EnumSource(RestartClusterManagerFlowState.class)
    void testGetUseCaseForRestartClusterManagerFlowState(RestartClusterManagerFlowState flowState) {
        Value expected = switch (flowState) {
            case RESTART_CLUSTER_MANAGER_FINISED_STATE -> DISABLE_ENCRYPTION_PROFILE_FINISHED;
            case RESTART_CLUSTER_MANAGER_FAILED_STATE -> DISABLE_ENCRYPTION_PROFILE_FAILED;
            default -> UNSET;
        };
        assertThat(underTest.getUseCaseForFlowState(flowState)).isEqualTo(expected);
    }
}
