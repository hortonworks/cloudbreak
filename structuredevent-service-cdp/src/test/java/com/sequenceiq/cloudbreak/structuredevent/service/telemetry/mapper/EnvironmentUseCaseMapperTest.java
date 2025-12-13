package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentStatus.Value.CCM_UPGRADE_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentStatus.Value.CCM_UPGRADE_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentStatus.Value.CCM_UPGRADE_STARTED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentStatus.Value.CREATE_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentStatus.Value.CREATE_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentStatus.Value.CREATE_STARTED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentStatus.Value.UNSET;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentStatus.Value;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.service.TestEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.TestFlowConfig;
import com.sequenceiq.cloudbreak.structuredevent.service.TestFlowState;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;

@ExtendWith(MockitoExtension.class)
class EnvironmentUseCaseMapperTest {

    @Spy
    private List<AbstractFlowConfiguration> flowConfigurations = new ArrayList<>();

    @Spy
    private List<FlowEventChainFactory> flowEventChainFactories = new ArrayList<>();

    @Spy
    private CDPRequestProcessingStepMapper cdpRequestProcessingStepMapper;

    @InjectMocks
    private EnvironmentUseCaseMapper underTest;

    @BeforeEach()
    void setUp() {
        flowConfigurations.add(new TestEnvironmentFlowConfig(TestFlowState.class, TestEvent.class));
        flowEventChainFactories.add(new EnvironmentUseCaseMapperTest.TestFlowEventChainFactory());
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
                mapFlowDetailsToUseCase("INIT_STATE", "TestEnvironmentFlowConfig"));
        assertEquals(CREATE_FINISHED,
                mapFlowDetailsToUseCase("FINISHED_STATE", "TestEnvironmentFlowConfig"));
        assertEquals(CREATE_FAILED,
                mapFlowDetailsToUseCase("FAILED_STATE", "TestEnvironmentFlowConfig"));
        assertEquals(UNSET,
                mapFlowDetailsToUseCase("TEMP_STATE", "TestEnvironmentFlowConfig"));
        assertEquals(UNSET,
                mapFlowDetailsToUseCase("NOT_THE_LATEST_FAILED_STATE", "TestEnvironmentFlowConfig"));
        assertEquals(UNSET,
                mapFlowDetailsToUseCase("FINAL_STATE", "TestEnvironmentFlowConfig"));
    }

    @Test
    void testIncorrectNextFlowStatesMappedToUnsetUseCase() {
        assertEquals(UNSET,
                mapFlowDetailsToUseCase("OTHER_STATE", "TestEnvironmentFlowConfig"));
        assertEquals(UNSET,
                mapFlowDetailsToUseCase(null, "TestEnvironmentFlowConfig"));
        assertEquals(UNSET,
                mapFlowDetailsToUseCase("", "TestEnvironmentFlowConfig"));
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

    private class TestFlowEventChainFactory implements FlowEventChainFactory, EnvironmentUseCaseAware {

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
                return CCM_UPGRADE_STARTED;
            } else if (flowState.toString().endsWith("FAILED_STATE")
                    && !flowState.equals(TestFlowState.NOT_THE_LATEST_FAILED_STATE)) {
                return CCM_UPGRADE_FAILED;
            } else if (flowState.equals(TestFlowState.FINISHED_STATE)) {
                return CCM_UPGRADE_FINISHED;
            }
            return UNSET;
        }
    }

    private class TestEnvironmentFlowConfig extends TestFlowConfig implements EnvironmentUseCaseAware {

        protected TestEnvironmentFlowConfig(Class stateType, Class eventType) {
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
    }
}
