package com.sequenceiq.cloudbreak.core.flow2.stack.provision;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.GCP;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.flowlog.FlowLogService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component("StartProvisioningRestartAction")
public class StartProvisioningRestartAction extends FillInMemoryStateStoreRestartAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartProvisioningRestartAction.class);

    @Inject
    private StackService stackService;

    @Inject
    private FlowLogService flowLogService;

    @Override
    public void restart(String flowId, String flowChainId, String event, Object payload) {
        Stack stack = null;
        try {
            Payload stackPayload = (Payload) payload;
            stack = stackService.getById(stackPayload.getStackId());
            if (stack.getPlatformVariant().equals(GCP)) {
                flowLogService.terminate(stackPayload.getStackId(), flowId);
            } else {
                restart(flowId, flowChainId, event, payload, stack);
            }
        } catch (Exception e) {
            if (stack != null) {
                MDCBuilder.buildMdcContext(stack);
            }
            LOGGER.error("Failed to restart stack provision", e);
        }
    }
}
