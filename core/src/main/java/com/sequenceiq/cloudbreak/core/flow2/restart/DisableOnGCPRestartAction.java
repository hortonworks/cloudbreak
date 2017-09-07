package com.sequenceiq.cloudbreak.core.flow2.restart;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.GCP;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.flowlog.FlowLogService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component("DisableOnGCPRestartAction")
public class DisableOnGCPRestartAction extends FillInMemoryStateStoreRestartAction {

    @Inject
    private StackService stackService;

    @Inject
    private FlowLogService flowLogService;

    @Override
    public void restart(String flowId, String flowChainId, String event, Object payload) {
        Payload stackPayload = (Payload) payload;
        Stack stack = stackService.getById(stackPayload.getStackId());
        if (stack.getPlatformVariant().equals(GCP)) {
            flowLogService.terminate(stackPayload.getStackId(), flowId);
        } else {
            restart(flowId, flowChainId, event, payload, stack);
        }
    }
}
