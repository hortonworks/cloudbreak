package com.sequenceiq.cloudbreak.core.flow2.stack.downscale;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.core.flow2.event.StackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.message.Msg;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.flow.StackScalingService;

@Service
public class StackDownscaleService {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackDownscaleService.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private StackScalingService stackScalingService;

    @Inject
    private StackService stackService;

    public void startStackDownscale(StackScalingFlowContext context, StackDownscaleTriggerEvent stackDownscaleTriggerEvent) {
        LOGGER.debug("Downscaling of stack {}", context.getStack().getId());
        stackUpdater.updateStackStatus(context.getStack().getId(), DetailedStackStatus.DOWNSCALE_IN_PROGRESS);
        Set<Long> privateIds = stackDownscaleTriggerEvent.getPrivateIds();
        List<String> instanceIdList = Collections.emptyList();
        if (privateIds != null) {
            Stack stack = stackService.getByIdWithListsInTransaction(context.getStack().getId());
            instanceIdList = stackService.getInstanceIdsForPrivateIds(stack.getInstanceMetaDataAsList(), privateIds);
        }
        Object msgParam = instanceIdList.isEmpty() ? Math.abs(stackDownscaleTriggerEvent.getAdjustment()) : instanceIdList;
        flowMessageService.fireEventAndLog(context.getStack().getId(), Msg.STACK_DOWNSCALE_INSTANCES, UPDATE_IN_PROGRESS.name(), msgParam);
    }

    public void finishStackDownscale(StackScalingFlowContext context, String instanceGroupName, Collection<String> instanceIds)
            throws TransactionExecutionException {
        Stack stack = context.getStack();
        InstanceGroup g = stack.getInstanceGroupByInstanceGroupName(instanceGroupName);
        stackScalingService.updateRemovedResourcesState(stack, instanceIds, g);
        List<String> fqdns = stack.getInstanceGroups()
                .stream().filter(ig -> ig.getGroupName().equals(instanceGroupName))
                .flatMap(instanceGroup -> instanceGroup.getInstanceMetaDataSet().stream())
                .map(InstanceMetaData::getInstanceId)
                .filter(instanceIds::contains)
                .collect(toList());
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.DOWNSCALE_COMPLETED,
                String.format("Downscale of the cluster infrastructure finished successfully. Terminated node(s): %s", fqdns));
        flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_DOWNSCALE_SUCCESS, AVAILABLE.name(), fqdns);
    }

    public void handleStackDownscaleError(StackFailureContext context, Exception errorDetails) {
        LOGGER.info("Exception during the downscaling of stack", errorDetails);
        flowMessageService.fireEventAndLog(context.getStackView().getId(), Msg.STACK_DOWNSCALE_FAILED, UPDATE_FAILED.name());
        stackUpdater.updateStackStatus(context.getStackView().getId(), DetailedStackStatus.DOWNSCALE_FAILED);
    }
}
