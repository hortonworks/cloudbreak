package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.CREATE_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.CREATE_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.CREATE_STARTED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UNSET;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UPSCALE_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UPSCALE_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UPSCALE_STARTED;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;

@ExtendWith(MockitoExtension.class)
class ClusterUseCaseMapperTest {

    @Spy
    private ClusterRequestProcessingStepMapper clusterRequestProcessingStepMapper;

    @Spy
    private List<AbstractFlowConfiguration> flowConfigurations = new ArrayList<>();

    @Spy
    private List<FlowEventChainFactory> flowEventChainFactories = new ArrayList<>();

    @InjectMocks
    private ClusterUseCaseMapper underTest;

    @BeforeEach
    void setUp() {
        flowConfigurations.add(new TestFlowConfig(TestFlowState.class, TestEvent.class));
        flowEventChainFactories.add(new TestFlowEventChainFactory());
        underTest.init();
    }

    @Test
    void testNullFlowDetailsMappedToUnset() {
        assertEquals(UNSET, underTest.useCase(null));
    }

    @Test
    void testEmptyFlowDetailsMappedToUnset() {
        assertEquals(UNSET, underTest.useCase(new FlowDetails()));
    }

    @Test
    void testCorrectNextFlowStatesMappedCorrectly() {
        assertEquals(CREATE_STARTED,
                mapFlowDetailsToUseCase("INIT_STATE", "TestFlowConfig"));
        assertEquals(CREATE_FINISHED,
                mapFlowDetailsToUseCase("FINISHED_STATE", "TestFlowConfig"));
        assertEquals(CREATE_FAILED,
                mapFlowDetailsToUseCase("FAILED_STATE", "TestFlowConfig"));
        assertEquals(UNSET,
                mapFlowDetailsToUseCase("TEMP_STATE", "TestFlowConfig"));
        assertEquals(UNSET,
                mapFlowDetailsToUseCase("NOT_THE_LATEST_FAILED_STATE", "TestFlowConfig"));
        assertEquals(UNSET,
                mapFlowDetailsToUseCase("FINAL_STATE", "TestFlowConfig"));
    }

    @Test
    void testCorrectNextFlowStatesInFlowChainMappedCorrectly() {
        assertEquals(UPSCALE_STARTED,
                mapFlowDetailsToUseCase("INIT_STATE", "TestFlowConfig", "TestFlowEventChainFactory"));
        assertEquals(UPSCALE_FINISHED,
                mapFlowDetailsToUseCase("FINISHED_STATE", "TestFlowConfig", "TestFlowEventChainFactory"));
        assertEquals(UPSCALE_FAILED,
                mapFlowDetailsToUseCase("FAILED_STATE", "TestFlowConfig", "TestFlowEventChainFactory"));
        assertEquals(UNSET,
                mapFlowDetailsToUseCase("TEMP_STATE", "TestFlowConfig", "TestFlowEventChainFactory"));
        assertEquals(UNSET,
                mapFlowDetailsToUseCase("NOT_THE_LATEST_FAILED_STATE", "TestFlowConfig", "TestFlowEventChainFactory"));
        assertEquals(UNSET,
                mapFlowDetailsToUseCase("FINAL_STATE", "TestFlowConfig", "TestFlowEventChainFactory"));
    }

    @Test
    void testIncorrectNextFlowStatesMappedToUnsetUseCase() {
        assertEquals(UNSET,
                mapFlowDetailsToUseCase("OTHER_STATE", "TestFlowConfig"));
        assertEquals(UNSET,
                mapFlowDetailsToUseCase(null, "TestFlowConfig"));
        assertEquals(UNSET,
                mapFlowDetailsToUseCase("", "TestFlowConfig"));
    }

    @Test
    void testIncorrectNextFlowStatesInFlowChainMappedToUnsetUseCase() {
        assertEquals(UNSET,
                mapFlowDetailsToUseCase("OTHER_STATE", "TestFlowConfig", "TestFlowEventChainFactory"));
        assertEquals(UNSET,
                mapFlowDetailsToUseCase(null, "TestFlowConfig", "TestFlowEventChainFactory"));
        assertEquals(UNSET,
                mapFlowDetailsToUseCase("", "TestFlowConfig", "TestFlowEventChainFactory"));
    }

    @Test
    void testIncorrectFlowConfigMappedToUnsetUseCase() {
        assertEquals(UNSET,
                mapFlowDetailsToUseCase("INIT_STATE", "OtherFlowConfig"));
        assertEquals(UNSET,
                mapFlowDetailsToUseCase("INIT_STATE", null));
        assertEquals(UNSET,
                mapFlowDetailsToUseCase("INIT_STATE", ""));
    }

    @Test
    void testIncorrectFlowConfigInFlowChainMappedToUnsetUseCase() {
        assertEquals(UNSET,
                mapFlowDetailsToUseCase("INIT_STATE", "OtherFlowConfig", "TestFlowEventChainFactory"));
        assertEquals(UNSET,
                mapFlowDetailsToUseCase("INIT_STATE", null, "TestFlowEventChainFactory"));
        assertEquals(UNSET,
                mapFlowDetailsToUseCase("INIT_STATE", "", "TestFlowEventChainFactory"));
    }

    @Test
    void testIncorrectFlowChainInFlowChainMappedToUnsetUseCase() {
        assertEquals(UNSET,
                mapFlowDetailsToUseCase("INIT_STATE", "TestFlowConfig", "OtherFlowEventChainFactory"));
    }

    private Value mapFlowDetailsToUseCase(String nextFlowState, String flowType) {
        return mapFlowDetailsToUseCase(nextFlowState, flowType, null);
    }

    private Value mapFlowDetailsToUseCase(String nextFlowState, String flowType, String flowChainType) {
        FlowDetails flowDetails = new FlowDetails();
        flowDetails.setNextFlowState(nextFlowState);
        flowDetails.setFlowType(flowType);
        flowDetails.setFlowChainType(flowChainType);
        return underTest.useCase(flowDetails);
    }

    private class TestFlowEventChainFactory implements FlowEventChainFactory, ClusterUseCaseAware {

        @Override
        public String initEvent() {
            return null;
        }

        @Override
        public FlowTriggerEventQueue createFlowTriggerEventQueue(Payload event) {
            return null;
        }

        @Override
        public Value getUseCaseForFlowState(Enum flowState) {
            if (flowState.equals(TestFlowState.INIT_STATE)) {
                return UPSCALE_STARTED;
            } else if (flowState.toString().endsWith("FAILED_STATE")
                    && !flowState.equals(TestFlowState.NOT_THE_LATEST_FAILED_STATE)) {
                return UPSCALE_FAILED;
            } else if (flowState.equals(TestFlowState.FINISHED_STATE)) {
                return UPSCALE_FINISHED;
            }
            return UNSET;
        }
    }

    private class TestFlowConfig extends AbstractFlowConfiguration<TestFlowState, TestEvent> implements ClusterUseCaseAware {

        protected TestFlowConfig(Class stateType, Class eventType) {
            super(stateType, eventType);
        }

        @Override
        public Value getUseCaseForFlowState(Enum<? extends FlowState> flowState) {
            if (flowState.equals(TestFlowState.INIT_STATE)) {
                return CREATE_STARTED;
            } else if (flowState.toString().endsWith("FAILED_STATE")
                    && !flowState.equals(TestFlowState.NOT_THE_LATEST_FAILED_STATE)) {
                return CREATE_FAILED;
            } else if (flowState.equals(TestFlowState.FINISHED_STATE)) {
                return CREATE_FINISHED;
            }
            return UNSET;
        }

        @Override
        protected List<Transition<TestFlowState, TestEvent>> getTransitions() {
            return null;
        }

        @Override
        protected FlowEdgeConfig getEdgeConfig() {
            return null;
        }

        @Override
        public TestEvent[] getEvents() {
            return new TestEvent[0];
        }

        @Override
        public TestEvent[] getInitEvents() {
            return new TestEvent[0];
        }

        @Override
        public String getDisplayName() {
            return null;
        }
    }

    private enum TestFlowState implements FlowState {
        INIT_STATE,
        TEMP_STATE,
        NOT_THE_LATEST_FAILED_STATE,
        FAILED_STATE,
        FINISHED_STATE,
        FINAL_STATE;

        @Override
        public Class<? extends RestartAction> restartAction() {
            return null;
        }
    }

    private class TestEvent implements FlowEvent {

        @Override
        public String name() {
            return null;
        }

        @Override
        public String event() {
            return null;
        }
    }
}
