package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value;
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
class DatalakeUseCaseMapperTest {

    @Spy
    private List<AbstractFlowConfiguration> flowConfigurations = new ArrayList<>();

    @Spy
    private List<FlowEventChainFactory> flowEventChainFactories = new ArrayList<>();

    @Spy
    private CDPRequestProcessingStepMapper cdpRequestProcessingStepMapper;

    @InjectMocks
    private DatalakeUseCaseMapper underTest;

    @BeforeEach()
    void setUp() {
        flowConfigurations.add(new DatalakeUseCaseMapperTest.TestDatalakeFlowConfig(TestFlowState.class, TestEvent.class));
        flowEventChainFactories.add(new DatalakeUseCaseMapperTest.TestFlowEventChainFactory());
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
                mapFlowDetailsToUseCase("INIT_STATE", "TestDatalakeFlowConfig"));
        assertEquals(CREATE_FINISHED,
                mapFlowDetailsToUseCase("FINISHED_STATE", "TestDatalakeFlowConfig"));
        assertEquals(CREATE_FAILED,
                mapFlowDetailsToUseCase("FAILED_STATE", "TestDatalakeFlowConfig"));
        assertEquals(UNSET,
                mapFlowDetailsToUseCase("TEMP_STATE", "TestDatalakeFlowConfig"));
        assertEquals(UNSET,
                mapFlowDetailsToUseCase("NOT_THE_LATEST_FAILED_STATE", "TestDatalakeFlowConfig"));
        assertEquals(UNSET,
                mapFlowDetailsToUseCase("FINAL_STATE", "TestDatalakeFlowConfig"));
    }

    @Test
    void testCorrectNextFlowStatesInFlowChainMappedCorrectly() {
        assertEquals(UPSCALE_STARTED,
                mapFlowDetailsToUseCase("INIT_STATE", "TestDatalakeFlowConfig", "TestFlowEventChainFactory"));
        assertEquals(UPSCALE_FINISHED,
                mapFlowDetailsToUseCase("FINISHED_STATE", "TestDatalakeFlowConfig", "TestFlowEventChainFactory"));
        assertEquals(UPSCALE_FAILED,
                mapFlowDetailsToUseCase("FAILED_STATE", "TestDatalakeFlowConfig", "TestFlowEventChainFactory"));
        assertEquals(UNSET,
                mapFlowDetailsToUseCase("TEMP_STATE", "TestDatalakeFlowConfig", "TestFlowEventChainFactory"));
        assertEquals(UNSET,
                mapFlowDetailsToUseCase("NOT_THE_LATEST_FAILED_STATE", "TestDatalakeFlowConfig", "TestFlowEventChainFactory"));
        assertEquals(UNSET,
                mapFlowDetailsToUseCase("FINAL_STATE", "TestDatalakeFlowConfig", "TestFlowEventChainFactory"));
    }

    @Test
    void testIncorrectNextFlowStatesMappedToUnsetUseCase() {
        assertEquals(UNSET,
                mapFlowDetailsToUseCase("OTHER_STATE", "TestDatalakeFlowConfig"));
        assertEquals(UNSET,
                mapFlowDetailsToUseCase(null, "TestDatalakeFlowConfig"));
        assertEquals(UNSET,
                mapFlowDetailsToUseCase("", "TestDatalakeFlowConfig"));
    }

    @Test
    void testIncorrectNextFlowStatesInFlowChainMappedToUnsetUseCase() {
        assertEquals(UNSET,
                mapFlowDetailsToUseCase("OTHER_STATE", "TestDatalakeFlowConfig", "TestFlowEventChainFactory"));
        assertEquals(UNSET,
                mapFlowDetailsToUseCase(null, "TestDatalakeFlowConfig", "TestFlowEventChainFactory"));
        assertEquals(UNSET,
                mapFlowDetailsToUseCase("", "TestDatalakeFlowConfig", "TestFlowEventChainFactory"));
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
                mapFlowDetailsToUseCase("INIT_STATE", "TestDatalakeFlowConfig", "OtherFlowEventChainFactory"));
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

    private class TestFlowEventChainFactory implements FlowEventChainFactory, DatalakeUseCaseAware {

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
                return Value.UPSCALE_STARTED;
            } else if (flowState.toString().endsWith("FAILED_STATE")
                    && !flowState.equals(TestFlowState.NOT_THE_LATEST_FAILED_STATE)) {
                return Value.UPSCALE_FAILED;
            } else if (flowState.equals(TestFlowState.FINISHED_STATE)) {
                return Value.UPSCALE_FINISHED;
            }
            return Value.UNSET;
        }
    }

    private class TestDatalakeFlowConfig extends TestFlowConfig implements DatalakeUseCaseAware {

        protected TestDatalakeFlowConfig(Class stateType, Class eventType) {
            super(stateType, eventType);
        }

        @Override
        public Value getUseCaseForFlowState(Enum<? extends FlowState> flowState) {
            if (flowState.equals(TestFlowState.INIT_STATE)) {
                return Value.CREATE_STARTED;
            } else if (flowState.toString().endsWith("FAILED_STATE")
                    && !flowState.equals(TestFlowState.NOT_THE_LATEST_FAILED_STATE)) {
                return Value.CREATE_FAILED;
            } else if (flowState.equals(TestFlowState.FINISHED_STATE)) {
                return Value.CREATE_FINISHED;
            }
            return Value.UNSET;
        }
    }

}