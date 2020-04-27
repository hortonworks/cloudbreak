package com.sequenceiq.cloudbreak.core.flow2.stack.downscale;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_DOWNSCALE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_DOWNSCALE_INSTANCES;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_DOWNSCALE_SUCCESS;
import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.core.flow2.event.StackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.flow.StackScalingService;
import com.sequenceiq.freeipa.api.v1.dns.DnsV1Endpoint;

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

    @Inject
    private DnsV1Endpoint dnsV1Endpoint;

    @Inject
    private WebApplicationExceptionMessageExtractor exceptionMessageExtractor;

    public void startStackDownscale(StackScalingFlowContext context, StackDownscaleTriggerEvent stackDownscaleTriggerEvent) {
        LOGGER.debug("Downscaling of stack {}", context.getStack().getId());
        stackUpdater.updateStackStatus(context.getStack().getId(), DetailedStackStatus.DOWNSCALE_IN_PROGRESS);
        Set<Long> privateIds = stackDownscaleTriggerEvent.getPrivateIds();
        List<String> instanceIdList = Collections.emptyList();
        if (privateIds != null) {
            Stack stack = stackService.getByIdWithListsInTransaction(context.getStack().getId());
            instanceIdList = stackService.getInstanceIdsForPrivateIds(stack.getInstanceMetaDataAsList(), privateIds);
        }
        String msgParam = instanceIdList.isEmpty() ? String.valueOf(Math.abs(stackDownscaleTriggerEvent.getAdjustment())) : String.join(",", instanceIdList);
        flowMessageService.fireEventAndLog(context.getStack().getId(), UPDATE_IN_PROGRESS.name(), STACK_DOWNSCALE_INSTANCES, msgParam);
    }

    public void finishStackDownscale(StackScalingFlowContext context, String instanceGroupName, Collection<String> instanceIds)
            throws TransactionExecutionException {
        Stack stack = context.getStack();
        InstanceGroup g = stack.getInstanceGroupByInstanceGroupName(instanceGroupName);
        stackScalingService.updateRemovedResourcesState(instanceIds, g);
        List<InstanceMetaData> instanceMetaDatas = stack.getInstanceGroups()
                .stream().filter(ig -> ig.getGroupName().equals(instanceGroupName))
                .flatMap(instanceGroup -> instanceGroup.getInstanceMetaDataSet().stream())
                .filter(im -> instanceIds.contains(im.getInstanceId()))
                .collect(toList());
        List<String> fqdns = instanceMetaDatas.stream().map(InstanceMetaData::getDiscoveryFQDN).collect(Collectors.toList());
        cleanupDnsRecords(stack, fqdns);
        List<String> deletedInstanceIds = instanceMetaDatas.stream()
                .map(instanceMetaData ->
                        instanceMetaData.getInstanceId() != null ? instanceMetaData.getInstanceId() : instanceMetaData.getPrivateId().toString())
                .collect(Collectors.toList());
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.DOWNSCALE_COMPLETED,
                String.format("Downscale of the cluster infrastructure finished successfully. Terminated node(s): %s", deletedInstanceIds));
        flowMessageService.fireEventAndLog(stack.getId(), AVAILABLE.name(), STACK_DOWNSCALE_SUCCESS, String.join(",", deletedInstanceIds));
    }

    private void cleanupDnsRecords(Stack stack, List<String> fqdns) {
        try {
            LOGGER.info("Cleanup DNS records for FQDNS: [{}]", fqdns);
            dnsV1Endpoint.deleteDnsRecordsByFqdn(stack.getEnvironmentCrn(), fqdns);
        } catch (WebApplicationException e) {
            String errorMessage = exceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error("Failed to delete dns records for FQDNS: [{}] with message: '{}'", fqdns, errorMessage, e);
        } catch (Exception e) {
            LOGGER.error("Failed to delete dns records for FQDNS: [{}]", fqdns, e);
        }
    }

    public void handleStackDownscaleError(StackFailureContext context, Exception errorDetails) {
        LOGGER.info("Exception during the downscaling of stack", errorDetails);
        flowMessageService.fireEventAndLog(context.getStackView().getId(), UPDATE_FAILED.name(), STACK_DOWNSCALE_FAILED);
        stackUpdater.updateStackStatus(context.getStackView().getId(), DetailedStackStatus.DOWNSCALE_FAILED);
    }
}
