package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper;

import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value;
import com.sequenceiq.flow.core.FlowState;

public interface ClusterUseCaseAware {

    Value getUseCaseForFlowState(Enum<? extends FlowState> flowState);
}
