package com.sequenceiq.cloudbreak.core.flow2.stack.downscale;

import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.model.Status.UPDATE_IN_PROGRESS;
import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.DetailedStackStatus;
import com.sequenceiq.cloudbreak.core.flow2.event.StackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.cluster.flow.EmailSenderService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.flow.StackScalingService;
import com.sequenceiq.cloudbreak.service.usages.UsageService;
import com.sequenceiq.cloudbreak.util.StackUtil;

@Service
public class StackDownscaleService {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackDownscaleService.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private FlowMessageService flowMessageService;

    @Inject
    private EmailSenderService emailSenderService;

    @Inject
    private StackScalingService stackScalingService;

    @Inject
    private UsageService usageService;

    @Inject
    private StackService stackService;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;

    public void startStackDownscale(StackScalingFlowContext context, StackDownscaleTriggerEvent stackDownscaleTriggerEvent) {
        LOGGER.debug("Downscaling of stack {}", context.getStack().getId());
        stackUpdater.updateStackStatus(context.getStack().getId(), DetailedStackStatus.DOWNSCALE_IN_PROGRESS);
        Set<Long> privateIds = stackDownscaleTriggerEvent.getPrivateIds();
        List<String> instanceIdList = Collections.emptyList();
        if (privateIds != null) {
            Stack stack = stackService.getByIdWithLists(context.getStack().getId());
            instanceIdList = stackService.getInstanceIdsForPrivateIds(stack.getInstanceMetaDataAsList(), privateIds);
        }
        Object msgParam = instanceIdList.isEmpty() ? Math.abs(stackDownscaleTriggerEvent.getAdjustment()) : instanceIdList;
        flowMessageService.fireEventAndLog(context.getStack().getId(), Msg.STACK_DOWNSCALE_INSTANCES, UPDATE_IN_PROGRESS.name(), msgParam);
    }

    public void finishStackDownscale(StackScalingFlowContext context, String instanceGroupName, Collection<String> instanceIds)
            throws TransactionExecutionException {
        Stack stack = context.getStack();
        InstanceGroup g = stack.getInstanceGroupByInstanceGroupName(instanceGroupName);
        Integer nodeCount = stackScalingService.updateRemovedResourcesState(stack, instanceIds, g);
        List<String> fqdns = instanceMetaDataRepository.findAllByInstanceIdIn(instanceIds).stream().map(InstanceMetaData::getInstanceId).collect(toList());
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.DOWNSCALE_COMPLETED,
                String.format("Downscale of the cluster infrastructure finished successfully. Terminated node(s): %s", fqdns));
        flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_DOWNSCALE_SUCCESS, AVAILABLE.name(), fqdns);

        if (stack.getCluster() != null && stack.getCluster().getEmailNeeded()) {
            emailSenderService.sendDownScaleSuccessEmail(stack.getCluster().getOwner(), stack.getCluster().getEmailTo(),
                    stackUtil.extractAmbariIp(stack), stack.getCluster().getName());
            flowMessageService.fireEventAndLog(context.getStack().getId(), Msg.STACK_NOTIFICATION_EMAIL, AVAILABLE.name());
        }
        usageService.scaleUsagesForStack(stack.getId(), instanceGroupName, nodeCount);
    }

    public void handleStackDownscaleError(Exception errorDetails) {
        LOGGER.error("Exception during the downscaling of stack", errorDetails);
    }
}
