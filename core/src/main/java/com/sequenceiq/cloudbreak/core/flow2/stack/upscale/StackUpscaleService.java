package com.sequenceiq.cloudbreak.core.flow2.stack.upscale;

import static com.sequenceiq.cloudbreak.api.model.InstanceStatus.CREATED;
import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.model.Status.UPDATE_IN_PROGRESS;
import static java.lang.String.format;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpscaleStackResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.common.type.BillingStatus;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.core.flow.context.StackScalingContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowFailureEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackScalingFlowContext;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetadataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.connector.OperationException;
import com.sequenceiq.cloudbreak.service.stack.flow.MetadataSetupService;
import com.sequenceiq.cloudbreak.service.stack.flow.StackScalingService;

@Service
public class StackUpscaleService {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackUpscaleService.class);
    @Inject
    private StackUpdater stackUpdater;
    @Inject
    private FlowMessageService flowMessageService;
    @Inject
    private StackScalingService stackScalingService;
    @Inject
    private StackService stackService;
    @Inject
    private InstanceMetaDataToCloudInstanceConverter metadataConverter;
    @Inject
    private ResourceToCloudResourceConverter cloudResourceConverter;
    @Inject
    private InstanceMetadataService instanceMetadataService;
    @Inject
    private InstanceGroupRepository instanceGroupRepository;
    @Inject
    private MetadataSetupService metadataSetupService;
    @Inject
    private ClusterService clusterService;
    @Inject
    private CloudbreakMessagesService messagesService;
    @Inject
    private CloudbreakEventService eventService;
    @Inject
    private HostGroupService hostGroupService;
    @Inject
    private StackToCloudStackConverter cloudStackConverter;


    public void startAddInstances(StackScalingFlowContext context, StackScalingContext payload) {
        MDCBuilder.buildMdcContext(context.getStack());
        String statusReason = format("Adding %s new instance(s) to the infrastructure.", payload.getScalingAdjustment());
        stackUpdater.updateStackStatus(payload.getStackId(), UPDATE_IN_PROGRESS, statusReason);
        flowMessageService.fireEventAndLog(context.getStack().getId(), Msg.STACK_ADDING_INSTANCES, UPDATE_IN_PROGRESS.name(), payload.getScalingAdjustment());
    }

    public StackScalingContext finishAddInstances(StackScalingFlowContext context, UpscaleStackResult payload) {
        LOGGER.info("Upscale stack result: {}", payload);
        List<CloudResourceStatus> results = payload.getResults();
        validateResourceResults(context.getCloudContext(), payload.getErrorDetails(), payload.getResults());
        updateNodeCount(context.getStack().getId(), context.getCloudStack().getGroups(), results, false);
        Set<Resource> resourceSet = transformResults(results, context.getStack());
        if (resourceSet.isEmpty()) {
            throw new OperationException("Failed to upscale the cluster since all create request failed: " + results.get(0).getStatusReason());
        }
        LOGGER.debug("Adding new instances to the stack is DONE");
        return new StackScalingContext(context.getStack().getId(), Platform.platform(context.getStack().cloudPlatform()),
                context.getAdjustment(), context.getInstanceGroupName(), resourceSet, context.getScalingType(), null);

    }

    public Set<String> finishExtendMetadata(StackScalingFlowContext context, CollectMetadataResult payload) {
        List<CloudVmMetaDataStatus> coreInstanceMetaData = payload.getResults();
        metadataSetupService.saveInstanceMetaData(context.getStack(), coreInstanceMetaData, CREATED);
        Set<String> upscaleCandidateAddresses = new HashSet<>();
        for (CloudVmMetaDataStatus cloudVmMetaDataStatus : coreInstanceMetaData) {
            upscaleCandidateAddresses.add(cloudVmMetaDataStatus.getMetaData().getPrivateIp());
        }
        InstanceGroup instanceGroup = instanceGroupRepository.findOneByGroupNameInStack(context.getStack().getId(), context.getInstanceGroupName());
        int nodeCount = instanceGroup.getNodeCount() + coreInstanceMetaData.size();
        instanceGroup.setNodeCount(nodeCount);
        instanceGroupRepository.save(instanceGroup);
        clusterService.updateClusterStatusByStackId(context.getStack().getId(), AVAILABLE);
        eventService.fireCloudbreakEvent(context.getStack().getId(), BillingStatus.BILLING_CHANGED.name(),
                messagesService.getMessage("stack.metadata.setup.billing.changed"));
        flowMessageService.fireEventAndLog(context.getStack().getId(), Msg.STACK_METADATA_EXTEND, AVAILABLE.name());

        return upscaleCandidateAddresses;
    }

    public HostGroupAdjustmentJson finishExtendConsulMetadata(StackScalingFlowContext context) {
        MDCBuilder.buildMdcContext(context.getStack());
        Cluster cluster = clusterService.retrieveClusterByStackId(context.getStack().getId());
        HostGroupAdjustmentJson hostGroupAdjustmentJson = new HostGroupAdjustmentJson();
        hostGroupAdjustmentJson.setWithStackUpdate(false);
        hostGroupAdjustmentJson.setScalingAdjustment(context.getAdjustment());
        if (cluster != null) {
            HostGroup hostGroup = hostGroupService.getByClusterIdAndInstanceGroupName(cluster.getId(), context.getInstanceGroupName());
            hostGroupAdjustmentJson.setHostGroup(hostGroup.getName());
        }
        stackUpdater.updateStackStatus(context.getStack().getId(), AVAILABLE, "Stack upscale has been finished successfully.");
        flowMessageService.fireEventAndLog(context.getStack().getId(), Msg.STACK_UPSCALE_FINISHED, AVAILABLE.name());

        return hostGroupAdjustmentJson;
    }

    public void handleStackUpscaleFailure(StackFailureContext context, FlowFailureEvent payload) {
        LOGGER.error("Exception during the downscaling of stack", payload.getException());
        try {
            String errorReason = payload.getException().getMessage();
            Stack stack = context.getStack();
            MDCBuilder.buildMdcContext(stack);
            stackUpdater.updateStackStatus(stack.getId(), AVAILABLE, "Stack update failed. " + errorReason);
            flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_INFRASTRUCTURE_UPDATE_FAILED, AVAILABLE.name(), errorReason);
        } catch (Exception e) {
            LOGGER.error("Exception during the handling of stack scaling failure: {}", e.getMessage());
        }

    }

    private Set<Resource> transformResults(List<CloudResourceStatus> cloudResourceStatuses, Stack stack) {
        Set<Resource> retSet = new HashSet<>();
        for (CloudResourceStatus cloudResourceStatus : cloudResourceStatuses) {
            if (!cloudResourceStatus.isFailed()) {
                CloudResource cloudResource = cloudResourceStatus.getCloudResource();
                Resource resource = new Resource(cloudResource.getType(), cloudResource.getName(), cloudResource.getReference(), cloudResource.getStatus(),
                        stack, null);
                retSet.add(resource);
            }
        }
        return retSet;
    }

    private void validateResourceResults(CloudContext cloudContext, Exception exception, List<CloudResourceStatus> results) {
        String action = "upscale";
        if (exception != null) {
            LOGGER.error(format("Failed to %s stack: %s", action, cloudContext), exception);
            throw new OperationException(exception);
        }
        if (results.size() == 1 && (results.get(0).isFailed() || results.get(0).isDeleted())) {
            throw new OperationException(format("Failed to %s the stack for %s due to: %s", action, cloudContext, results.get(0).getStatusReason()));
        }
    }

    private void updateNodeCount(Long stackId, List<Group> originalGroups, List<CloudResourceStatus> statuses, boolean create) {
        for (Group group : originalGroups) {
            int nodeCount = group.getInstances().size();
            List<CloudResourceStatus> failedResources = removeFailedMetadata(stackId, statuses, group);
            if (!failedResources.isEmpty() && create) {
                int failedCount = failedResources.size();
                InstanceGroup instanceGroup = instanceGroupRepository.findOneByGroupNameInStack(stackId, group.getName());
                instanceGroup.setNodeCount(nodeCount - failedCount);
                instanceGroupRepository.save(instanceGroup);
                flowMessageService.fireEventAndLog(stackId, Msg.STACK_INFRASTRUCTURE_ROLLBACK_MESSAGE, Status.UPDATE_IN_PROGRESS.name(),
                        failedCount, group.getName(), failedResources.get(0).getStatusReason());
            }
        }
    }

    private List<CloudResourceStatus> removeFailedMetadata(long stackId, List<CloudResourceStatus> statuses, Group group) {
        Map<Long, CloudResourceStatus> failedResources = new HashMap<>();
        Set<Long> groupPrivateIds = getPrivateIds(group);
        for (CloudResourceStatus status : statuses) {
            Long privateId = status.getPrivateId();
            if (privateId != null && status.isFailed() && !failedResources.containsKey(privateId) && groupPrivateIds.contains(privateId)) {
                failedResources.put(privateId, status);
                instanceMetadataService.deleteInstanceRequest(stackId, privateId);
            }
        }
        return new ArrayList<>(failedResources.values());
    }

    private Set<Long> getPrivateIds(Group group) {
        Set<Long> ids = new HashSet<>();
        for (CloudInstance cloudInstance : group.getInstances()) {
            ids.add(cloudInstance.getTemplate().getPrivateId());
        }
        return ids;
    }

    public List<CloudInstance> getNewInstances(Stack stack) {
        List<CloudInstance> cloudInstances = cloudStackConverter.buildInstances(stack);
        Iterator<CloudInstance> iterator = cloudInstances.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getTemplate().getStatus() != InstanceStatus.CREATE_REQUESTED) {
                iterator.remove();
            }
        }
        return cloudInstances;
    }
}
