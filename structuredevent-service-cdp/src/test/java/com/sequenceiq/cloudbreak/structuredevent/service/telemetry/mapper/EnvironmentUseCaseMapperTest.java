package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentStatus.Value.CREATE_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentStatus.Value.CREATE_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentStatus.Value.CREATE_STARTED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentStatus.Value.UNSET;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentStatus.Value;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;

@ExtendWith(MockitoExtension.class)
class EnvironmentUseCaseMapperTest {

    @Spy
    private List<AbstractFlowConfiguration> flowConfigurations = new ArrayList<>();

    @Spy
    private CDPRequestProcessingStepMapper cdpRequestProcessingStepMapper;

    @InjectMocks
    private EnvironmentUseCaseMapper underTest;

    @BeforeEach()
    void setUp() {
        flowConfigurations.add(new TestFlowConfig(TestFlowState.class, TestEvent.class));
        underTest.init();
    }

    @Test
    void testNullFlowDetailsMappedToUnset() {
        Assertions.assertEquals(UNSET, underTest.useCase(null));
    }

    @Test
    void testEmptyFlowDetailsMappedToUnset() {
        Assertions.assertEquals(UNSET, underTest.useCase(new FlowDetails()));
    }

    @Test
    void testFlowChainTypeIsIgnored() {
        Assertions.assertEquals(CREATE_STARTED,
                mapFlowDetailsToUseCase("INIT_STATE", "TestFlowConfig", ""));
        Assertions.assertEquals(CREATE_STARTED,
                mapFlowDetailsToUseCase("INIT_STATE", "TestFlowConfig", "TestFlowChain"));
    }

    @Test
    void testCorrectNextFlowStatesMappedCorrectly() {
        Assertions.assertEquals(CREATE_STARTED,
                mapFlowDetailsToUseCase("INIT_STATE", "TestFlowConfig"));
        Assertions.assertEquals(CREATE_FINISHED,
                mapFlowDetailsToUseCase("FINISHED_STATE", "TestFlowConfig"));
        Assertions.assertEquals(CREATE_FAILED,
                mapFlowDetailsToUseCase("FAILED_STATE", "TestFlowConfig"));
        Assertions.assertEquals(UNSET,
                mapFlowDetailsToUseCase("TEMP_STATE", "TestFlowConfig"));
        Assertions.assertEquals(UNSET,
                mapFlowDetailsToUseCase("NOT_THE_LATEST_FAILED_STATE", "TestFlowConfig"));
        Assertions.assertEquals(UNSET,
                mapFlowDetailsToUseCase("FINAL_STATE", "TestFlowConfig"));
    }

    @Test
    void testIncorrectNextFlowStatesMappedToUnsetUseCase() {
        Assertions.assertEquals(UNSET,
                mapFlowDetailsToUseCase("OTHER_STATE", "TestFlowConfig"));
        Assertions.assertEquals(UNSET,
                mapFlowDetailsToUseCase(null, "TestFlowConfig"));
        Assertions.assertEquals(UNSET,
                mapFlowDetailsToUseCase("", "TestFlowConfig"));
    }

    @Test
    void testIncorrectFlowConfigMappedToUnsetUseCase() {
        Assertions.assertEquals(UNSET,
                mapFlowDetailsToUseCase("INIT_STATE", "OtherFlowConfig"));
        Assertions.assertEquals(UNSET,
                mapFlowDetailsToUseCase("INIT_STATE", null));
        Assertions.assertEquals(UNSET,
                mapFlowDetailsToUseCase("INIT_STATE", ""));
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

    private class TestFlowConfig extends AbstractFlowConfiguration<TestFlowState, TestEvent> implements EnvironmentUseCaseAware {

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
