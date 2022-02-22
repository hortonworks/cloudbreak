package com.sequenceiq.cloudbreak.core.flow2.stack.upscale;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.CREATED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_ADDING_INSTANCES;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_INFRASTRUCTURE_UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_METADATA_EXTEND_WITH_COUNT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_REPAIR_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_UPSCALE_QUOTA_ISSUE;
import static java.lang.String.format;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.exception.QuotaExceededException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackScalingFlowContext;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.UpscaleStackRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.UpscaleStackResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.OperationException;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.flow.MetadataSetupService;
import com.sequenceiq.cloudbreak.service.stack.flow.TlsSetupService;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.common.api.type.CommonResourceType;

@Service
public class StackUpscaleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackUpscaleService.class);

    private static final double ONE_HUNDRED = 100;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private MetadataSetupService metadataSetupService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private TlsSetupService tlsSetupService;

    @Inject
    private TransactionService transactionService;

    @Inject
    private StackScalabilityCondition stackScalabilityCondition;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    public void startAddInstances(Stack stack, Integer scalingAdjustment, String hostGroupName) {
        String statusReason = format("Adding %s new instance(s) to instance group %s", scalingAdjustment, hostGroupName);
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.ADDING_NEW_INSTANCES, statusReason);
    }

    public void addInstanceFireEventAndLog(Stack stack, Integer scalingAdjustment, String hostGroupName,
            AdjustmentTypeWithThreshold adjustmentTypeWithThreshold) {
        flowMessageService.fireEventAndLog(stack.getId(), UPDATE_IN_PROGRESS.name(), STACK_ADDING_INSTANCES, String.valueOf(scalingAdjustment), hostGroupName,
                String.valueOf(adjustmentTypeWithThreshold.getAdjustmentType()), String.valueOf(adjustmentTypeWithThreshold.getThreshold()));
    }

    public void finishAddInstances(StackScalingFlowContext context, UpscaleStackResult payload) {
        LOGGER.debug("Upscale stack result: {}", payload);
        List<CloudResourceStatus> results = payload.getResults();
        validateResourceResults(context, payload.getErrorDetails(), results);
        Set<Long> successfulPrivateIds = results.stream().map(CloudResourceStatus::getPrivateId).filter(Objects::nonNull).collect(Collectors.toSet());
        metadataSetupService.cleanupRequestedInstancesIfNotInList(context.getStack().getId(), context.getInstanceGroupName(), successfulPrivateIds);
        if (successfulPrivateIds.isEmpty()) {
            metadataSetupService.cleanupRequestedInstancesWithoutFQDN(context.getStack().getId(), context.getInstanceGroupName());
            throw new OperationException("Failed to upscale the cluster since all create request failed. Resource set is empty");
        }
        LOGGER.debug("Adding new instances to the stack is DONE");
    }

    public void extendingMetadata(Stack stack) {
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.EXTENDING_METADATA);
    }

    void reRegisterWithClusterProxy(long stackId) {
        clusterService.updateClusterStatusByStackId(stackId, DetailedStackStatus.REGISTERING_TO_CLUSTER_PROXY,
                "Re-registering with Cluster Proxy service.");
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_RE_REGISTER_WITH_CLUSTER_PROXY);
    }

    public Set<String> finishExtendMetadata(Stack stack, Integer scalingAdjustment, CollectMetadataResult payload) throws TransactionExecutionException {
        return transactionService.required(() -> {
            List<CloudVmMetaDataStatus> coreInstanceMetaData = payload.getResults();
            int newInstances = metadataSetupService.saveInstanceMetaData(stack, coreInstanceMetaData, CREATED);
            Set<String> upscaleCandidateAddresses = coreInstanceMetaData.stream().filter(im -> im.getMetaData().getPrivateIp() != null)
                    .map(im -> im.getMetaData().getPrivateIp()).collect(Collectors.toSet());
            try {
                clusterService.updateClusterStatusByStackIdOutOfTransaction(stack.getId(), DetailedStackStatus.EXTENDING_METADATA_FINISHED);
            } catch (TransactionExecutionException e) {
                throw e.getCause();
            }
            if (scalingAdjustment != newInstances) {
                flowMessageService.fireEventAndLog(stack.getId(), AVAILABLE.name(), STACK_METADATA_EXTEND_WITH_COUNT, String.valueOf(newInstances),
                        String.valueOf(scalingAdjustment));
            }
            return upscaleCandidateAddresses;
        });
    }

    public void setupTls(StackContext context) throws CloudbreakException {
        Stack stack = context.getStack();
        for (InstanceMetaData gwInstance : stack.getNotTerminatedAndNotZombieGatewayInstanceMetadata()) {
            if (!stack.getTunnel().useCcm() && CREATED.equals(gwInstance.getInstanceStatus())) {
                tlsSetupService.setupTls(stack, gwInstance);
            }
        }
    }

    public void bootstrappingNewNodes(Stack stack) {
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.BOOTSTRAPPING_NEW_NODES);
    }

    public void extendingHostMetadata(Stack stack) {
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.EXTENDING_HOST_METADATA);
    }

    public void finishExtendHostMetadata(Stack stack) {
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.STACK_UPSCALE_COMPLETED, "Stack upscale has been finished successfully.");
    }

    public void handleStackUpscaleFailure(Boolean upscaleForRepair, Set<String> hostNames, Exception exception, Long stackId, String instanceGroupName) {
        LOGGER.info("Exception during the upscale of stack", exception);
        try {
            String errorReason = exception.getMessage();
            if (!upscaleForRepair) {
                metadataSetupService.cleanupRequestedInstancesWithoutFQDN(stackId, instanceGroupName);
                stackUpdater.updateStackStatus(stackId, DetailedStackStatus.UPSCALE_FAILED, "Stack update failed. " + errorReason);
                flowMessageService.fireEventAndLog(stackId, UPDATE_FAILED.name(), STACK_INFRASTRUCTURE_UPDATE_FAILED, errorReason);
            } else {
                metadataSetupService.handleRepairFail(stackId, hostNames);
                stackUpdater.updateStackStatus(stackId, DetailedStackStatus.REPAIR_FAILED, "Stack repair failed. " + errorReason);
                flowMessageService.fireEventAndLog(stackId, UPDATE_FAILED.name(), STACK_REPAIR_FAILED, errorReason);
            }
        } catch (Exception e) {
            LOGGER.info("Exception during the handling of stack scaling failure: {}", e.getMessage());
        }
    }

    private void validateResourceResults(StackScalingFlowContext context, Exception exception, List<CloudResourceStatus> results) {
        if (exception != null) {
            LOGGER.info(format("Failed to upscale stack: %s", context.getCloudContext()), exception);
            throw new OperationException(exception);
        }
        List<CloudResourceStatus> templates = results.stream().filter(result -> CommonResourceType.TEMPLATE == result.getCloudResource().getType()
                .getCommonResourceType()).collect(Collectors.toList());
        if (!templates.isEmpty() && (templates.get(0).isFailed() || templates.get(0).isDeleted())) {
            throw new OperationException(format("Failed to upscale the stack for %s due to: %s",
                    context.getCloudContext(), templates.get(0).getStatusReason()));
        }
    }

    public int getInstanceCountToCreate(Stack stack, String instanceGroupName, int adjustment, boolean repair) {
        Set<InstanceMetaData> unusedInstanceMetadata = instanceMetaDataService.unusedInstancesInInstanceGroupByName(stack.getId(), instanceGroupName);
        int reusableInstanceCount = repair ? 0 : unusedInstanceMetadata.size();
        return stackScalabilityCondition.isScalable(stack, instanceGroupName) ? adjustment - reusableInstanceCount : 0;
    }

    public List<CloudResourceStatus> upscale(AuthenticatedContext ac, UpscaleStackRequest<UpscaleStackResult> request, CloudConnector<?> connector)
            throws QuotaExceededException {
        CloudStack cloudStack = request.getCloudStack();
        AdjustmentTypeWithThreshold adjustmentTypeWithThreshold = request.getAdjustmentWithThreshold();
        try {
            return connector.resources().upscale(ac, cloudStack, request.getResourceList(), adjustmentTypeWithThreshold);
        } catch (QuotaExceededException quotaExceededException) {
            return handleQuotaExceptionAndRetryUpscale(request, connector, ac, cloudStack, adjustmentTypeWithThreshold, quotaExceededException);
        }
    }

    private List<CloudResourceStatus> handleQuotaExceptionAndRetryUpscale(UpscaleStackRequest<UpscaleStackResult> request, CloudConnector<?> connector,
            AuthenticatedContext ac, CloudStack cloudStack, AdjustmentTypeWithThreshold adjustmentTypeWithThreshold,
            QuotaExceededException quotaExceededException) throws QuotaExceededException {
        flowMessageService.fireEventAndLog(request.getResourceId(), UPDATE_IN_PROGRESS.name(), STACK_UPSCALE_QUOTA_ISSUE,
                quotaExceededException.getQuotaErrorMessage());
        List<Group> groups = cloudStack.getGroups();
        int removableNodeCount = getRemovableNodeCount(adjustmentTypeWithThreshold, quotaExceededException, groups);
        decreaseInstances(groups, removableNodeCount);
        return connector.resources().upscale(ac, cloudStack, request.getResourceList(),
                adjustmentTypeWithThreshold);
    }

    private int getRemovableNodeCount(AdjustmentTypeWithThreshold adjustmentTypeWithThreshold, QuotaExceededException quotaExceededException,
            List<Group> groups) {
        int additionalRequired = quotaExceededException.getAdditionalRequired();
        int originalRequestedNodeCount = groups.stream().flatMap(group -> group.getInstances().stream())
                .filter(cloudInstance -> cloudInstance.getInstanceId() == null)
                .collect(Collectors.toList())
                .size();
        if (originalRequestedNodeCount < 1) {
            return 0;
        }
        int cpuCountPerNode = additionalRequired / originalRequestedNodeCount;
        int removableCpuCount = additionalRequired + quotaExceededException.getCurrentUsage() - quotaExceededException.getCurrentLimit();
        int removableNodeCount = (int) Math.ceil((double) removableCpuCount / cpuCountPerNode);

        long provisionableNodeCount = originalRequestedNodeCount - removableNodeCount;
        switch (adjustmentTypeWithThreshold.getAdjustmentType()) {
            case EXACT:
                if (adjustmentTypeWithThreshold.getThreshold() > provisionableNodeCount) {
                    throw new CloudConnectorException(originalRequestedNodeCount + " nodes were requested, but the provisionable node " +
                            "count is " + provisionableNodeCount + ". Threshold is " + adjustmentTypeWithThreshold.getThreshold() +
                            ". Your quota limit exceeded on provider side.", quotaExceededException.getCause());
                }
                break;
            case PERCENTAGE:
                double calculatedPercentage = calculatePercentage(removableNodeCount, originalRequestedNodeCount);
                if (adjustmentTypeWithThreshold.getThreshold() > calculatedPercentage) {
                    throw new CloudConnectorException(originalRequestedNodeCount + " nodes were requested, but the provisionable node " +
                            "count is " + provisionableNodeCount + ". Threshold is " + adjustmentTypeWithThreshold.getThreshold() +
                            "%. Your quota limit exceeded on provider side.", quotaExceededException.getCause());
                }
                break;
            case BEST_EFFORT:
                if (provisionableNodeCount < 1) {
                    throw new CloudConnectorException("We are not able to provision any node. Your quota limit exceeded on provider side.",
                            quotaExceededException.getCause());
                }
                break;
            default:
                throw new CloudConnectorException("Unkown adjustment type: " + adjustmentTypeWithThreshold.getAdjustmentType(),
                        quotaExceededException.getCause());
        }
        return removableNodeCount;
    }

    private double calculatePercentage(double removableNodeCount, double requestedNodeCount) {
        double calculatedPercentage = (requestedNodeCount - removableNodeCount) / requestedNodeCount * ONE_HUNDRED;
        LOGGER.info("Calculated percentage: {}", calculatedPercentage);
        return calculatedPercentage;
    }

    private void decreaseInstances(List<Group> groups, int removableNodeCount) {
        for (Group group : groups) {
            List<CloudInstance> newGroupInstances = group.getInstances().stream()
                    .filter(cloudInstance -> cloudInstance.getInstanceId() == null)
                    .collect(Collectors.toList());
            int removableNodeCountForGroup = Integer.min(newGroupInstances.size(), removableNodeCount);
            int fromIndex = newGroupInstances.size() - removableNodeCountForGroup;
            List<CloudInstance> removableInstances = newGroupInstances.subList(fromIndex, newGroupInstances.size());
            removableNodeCount = removableNodeCount - removableInstances.size();
            group.getInstances().removeAll(removableInstances);
        }
    }

}
