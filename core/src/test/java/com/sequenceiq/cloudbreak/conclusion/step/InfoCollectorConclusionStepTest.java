package com.sequenceiq.cloudbreak.conclusion.step;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cedarsoftware.util.io.JsonWriter;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.type.ScalingType;
import com.sequenceiq.cloudbreak.core.flow2.event.StackAndClusterUpscaleTriggerEvent;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stackstatus.StackStatusService;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.flow.domain.ClassValue;
import com.sequenceiq.flow.domain.FlowChainLog;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.service.flowlog.FlowChainLogService;
import com.sequenceiq.flow.service.flowlog.FlowLogDBService;

@ExtendWith(MockitoExtension.class)
class InfoCollectorConclusionStepTest {

    private static final String FLOW_ID = "flowId";

    private static final String FLOW_CHAIN_ID = "flowChainId";

    private static final Long STACK_ID = 1L;

    @Mock
    private FlowLogDBService flowLogDBService;

    @Mock
    private FlowChainLogService flowChainLogService;

    @Mock
    private StackStatusService stackStatusService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @InjectMocks
    private InfoCollectorConclusionStep underTest;

    @Test
    public void testCollectDebugInfo() {
        when(flowLogDBService.findAllByResourceIdAndFinalizedIsFalseOrderByCreatedDesc(eq(STACK_ID))).thenReturn(List.of(
                new FlowLog(STACK_ID, FLOW_ID, FLOW_CHAIN_ID, "user", "NEXT_EVENT", "payload", "payloadJackson",
                        ClassValue.ofUnknown("PayloadType"), "variables", null, ClassValue.ofUnknown("FlowType"), "CURRENT_STATE")
        ));
        Queue<Selectable> queue = new LinkedBlockingQueue<>();
        queue.add(new StackAndClusterUpscaleTriggerEvent("selector", STACK_ID, new HashMap<>(), ScalingType.UPSCALE_TOGETHER, null, null, null));
        String chain = JsonWriter.objectToJson(queue);
        FlowChainLog flowChainLog = new FlowChainLog("flowChainType", FLOW_CHAIN_ID, "parentFLowChainId", chain, null,
                "user", "triggerEvent", null);
        when(flowChainLogService.findByFlowChainIdOrderByCreatedDesc(eq(FLOW_CHAIN_ID))).thenReturn(List.of(flowChainLog));
        when(flowChainLogService.getRelatedFlowChainLogs(anyList())).thenReturn(List.of(flowChainLog));
        when(stackStatusService.findAllStackStatusesById(eq(STACK_ID), anyLong())).thenReturn(List.of(
                new StackStatus(null, Status.AVAILABLE, "available", DetailedStackStatus.AVAILABLE),
                new StackStatus(null, Status.UPDATE_IN_PROGRESS, "requested", DetailedStackStatus.UPSCALE_REQUESTED),
                new StackStatus(null, Status.UPDATE_FAILED, "error", DetailedStackStatus.UPSCALE_FAILED)
        ));
        InstanceGroup instanceGroup = TestUtil.instanceGroup(1L, "worker", InstanceGroupType.CORE, new Template());
        when(instanceMetaDataService.getAllNotTerminatedInstanceMetadataViewsByStackId(STACK_ID))
                .thenReturn(List.copyOf(TestUtil.generateInstanceMetaDatas(3, 1L, instanceGroup)));

        Conclusion check = underTest.check(STACK_ID);
        assertFalse(check.isFailureFound());

        verify(flowLogDBService, times(1)).findAllByResourceIdAndFinalizedIsFalseOrderByCreatedDesc(eq(STACK_ID));
        verify(flowChainLogService, times(1)).findByFlowChainIdOrderByCreatedDesc(eq(FLOW_CHAIN_ID));
        verify(flowChainLogService, times(1)).getRelatedFlowChainLogs(eq(List.of(flowChainLog)));
        verify(stackStatusService, times(1)).findAllStackStatusesById(eq(STACK_ID), anyLong());
        verify(instanceMetaDataService, times(1)).getAllNotTerminatedInstanceMetadataViewsByStackId(eq(STACK_ID));
    }

}