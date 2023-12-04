package com.sequenceiq.cloudbreak.structuredevent.service;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.CREATE_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.CREATE_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.CREATE_STARTED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UNSET;

import java.util.List;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.ClusterUseCaseAware;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;

public class TestFlowConfig extends AbstractFlowConfiguration<TestFlowState, TestEvent> implements ClusterUseCaseAware {

    public TestFlowConfig(Class stateType, Class eventType) {
        super(stateType, eventType);
    }

    @Override
    public UsageProto.CDPClusterStatus.Value getUseCaseForFlowState(Enum<? extends FlowState> flowState) {
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
    public FlowEdgeConfig getEdgeConfig() {
        return new FlowEdgeConfig(TestFlowState.INIT_STATE, TestFlowState.FINAL_STATE, TestFlowState.FAILED_STATE, TestEvent.FAIL_HANDLED_EVENT);
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
