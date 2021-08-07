package com.sequenceiq.cloudbreak.core.flow2.stack.upscale;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.CREATED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_ADDING_INSTANCES;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_INFRASTRUCTURE_UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_METADATA_EXTEND_WITH_COUNT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_REPAIR_FAILED;
import static com.sequenceiq.cloudbreak.util.Benchmark.measure;
import static java.lang.String.format;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpscaleStackResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackScalingFlowContext;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.OperationException;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.stack.flow.MetadataSetupService;
import com.sequenceiq.cloudbreak.service.stack.flow.TlsSetupService;
import com.sequenceiq.common.api.type.CommonResourceType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Service
public class StackUpscaleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackUpscaleService.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private MetadataSetupService metadataSetupService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private StackToCloudStackConverter cloudStackConverter;

    @Inject
    private TlsSetupService tlsSetupService;

    @Inject
    private TransactionService transactionService;

    @Inject
    private EnvironmentClientService environmentClientService;

    public void startAddInstances(Stack stack, Integer scalingAdjustment, String hostGroupName) {
        String statusReason = format("Adding %s new instance(s) to instance group %s", scalingAdjustment, hostGroupName);
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.ADDING_NEW_INSTANCES, statusReason);
    }

    public void addInstanceFireEventAndLog(Stack stack, Integer scalingAdjustment, String hostGroupName) {
        flowMessageService.fireEventAndLog(stack.getId(), UPDATE_IN_PROGRESS.name(), STACK_ADDING_INSTANCES, String.valueOf(scalingAdjustment), hostGroupName);
    }

    public void finishAddInstances(StackScalingFlowContext context, UpscaleStackResult payload) {
        LOGGER.debug("Upscale stack result: {}", payload);
        List<CloudResourceStatus> results = payload.getResults();
        validateResourceResults(context, payload.getErrorDetails(), results);
        Set<Resource> resourceSet = transformResults(results, context.getStack());
        if (resourceSet.isEmpty()) {
            metadataSetupService.cleanupRequestedInstances(context.getStack(), context.getInstanceGroupName());
            throw new OperationException("Failed to upscale the cluster since all create request failed. Resource set is empty");
        }
        LOGGER.debug("Adding new instances to the stack is DONE");
    }

    public void extendingMetadata(Stack stack) {
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.EXTENDING_METADATA);
        clusterService.updateClusterStatusByStackId(stack.getId(), UPDATE_IN_PROGRESS);
    }

    void reRegisterWithClusterProxy(long stackId) {
        clusterService.updateClusterStatusByStackId(stackId, UPDATE_IN_PROGRESS, "Re-registering with Cluster Proxy service.");
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_RE_REGISTER_WITH_CLUSTER_PROXY);
    }

    public Set<String> finishExtendMetadata(Stack stack, Integer scalingAdjustment, CollectMetadataResult payload) throws TransactionExecutionException {
        return transactionService.required(() -> {
            List<CloudVmMetaDataStatus> coreInstanceMetaData = payload.getResults();
            int newInstances = metadataSetupService.saveInstanceMetaData(stack, coreInstanceMetaData, CREATED);
            Set<String> upscaleCandidateAddresses = coreInstanceMetaData.stream().filter(im -> im.getMetaData().getPrivateIp() != null)
                    .map(im -> im.getMetaData().getPrivateIp()).collect(Collectors.toSet());
            try {
                clusterService.updateClusterStatusByStackIdOutOfTransaction(stack.getId(), AVAILABLE);
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
        for (InstanceMetaData gwInstance : stack.getNotTerminatedGatewayInstanceMetadata()) {
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
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.UPSCALE_COMPLETED, "Stack upscale has been finished successfully.");
    }

    public void handleStackUpscaleFailure(Boolean upscaleForRepair, Exception exception, Long stackId) {
        LOGGER.info("Exception during the upscale of stack", exception);
        try {
            String errorReason = exception.getMessage();
            metadataSetupService.cleanupRequestedInstances(stackId);
            if (!upscaleForRepair) {
                stackUpdater.updateStackStatus(stackId, DetailedStackStatus.UPSCALE_FAILED, "Stack update failed. " + errorReason);
                flowMessageService.fireEventAndLog(stackId, UPDATE_FAILED.name(), STACK_INFRASTRUCTURE_UPDATE_FAILED, errorReason);
            } else {
                stackUpdater.updateStackStatus(stackId, DetailedStackStatus.REPAIR_FAILED, "Stack repair failed. " + errorReason);
                flowMessageService.fireEventAndLog(stackId, UPDATE_FAILED.name(), STACK_REPAIR_FAILED, errorReason);
            }
        } catch (Exception e) {
            LOGGER.info("Exception during the handling of stack scaling failure: {}", e.getMessage());
        }
    }

    private Set<Resource> transformResults(Iterable<CloudResourceStatus> cloudResourceStatuses, Stack stack) {
        Set<Resource> retSet = new HashSet<>();
        for (CloudResourceStatus cloudResourceStatus : cloudResourceStatuses) {
            if (!cloudResourceStatus.isFailed()) {
                CloudResource cloudResource = cloudResourceStatus.getCloudResource();
                Resource resource = new Resource(
                        cloudResource.getType(),
                        cloudResource.getName(),
                        cloudResource.getReference(),
                        cloudResource.getStatus(),
                        stack,
                        null,
                        cloudResource.getAvailabilityZone());
                retSet.add(resource);
            }
        }
        return retSet;
    }

    private void validateResourceResults(StackScalingFlowContext context, Exception exception, List<CloudResourceStatus> results) {
        if (exception != null) {
            LOGGER.info(format("Failed to upscale stack: %s", context.getCloudContext()), exception);
            throw new OperationException(exception);
        }
        List<CloudResourceStatus> templates = results.stream().filter(result -> CommonResourceType.TEMPLATE == result.getCloudResource().getType()
                .getCommonResourceType()).collect(Collectors.toList());
        if (!templates.isEmpty() && (templates.get(0).isFailed() || templates.get(0).isDeleted())) {
            throw new OperationException(format("Failed to upscale the stack for %s due to: %s", context.getCloudContext(), templates.get(0).getStatusReason()));
        }
    }

    public List<CloudInstance> buildNewInstances(Stack stack, String extendedInstanceGroupName, int count) {
        LOGGER.info("Build {} instances for group: {}", count, extendedInstanceGroupName);
        Optional<InstanceGroup> extendedInstanceGroup = stack.getInstanceGroups().stream()
                .filter(instanceGroup -> extendedInstanceGroupName.equals(instanceGroup.getGroupName()))
                .findAny();
        long privateId = getFirstValidPrivateId(stack.getInstanceGroupsAsList());
        List<CloudInstance> newInstances = new ArrayList<>();
        if (extendedInstanceGroup.isPresent()) {
            for (long i = 0; i < count; i++) {
                newInstances.add(cloudStackConverter.buildInstance(
                        null,
                        extendedInstanceGroup.get(),
                        stack.getStackAuthentication(),
                        privateId++,
                        InstanceStatus.CREATE_REQUESTED,
                        getEnvironmentByEnvironmentCrn(stack.getEnvironmentCrn())));
            }
        }
        LOGGER.debug("New instances have been built: {}", newInstances.stream().map(CloudInstance::getInstanceId).collect(Collectors.toList()));
        return newInstances;
    }

    private DetailedEnvironmentResponse getEnvironmentByEnvironmentCrn(String environmentCrn) {
        DetailedEnvironmentResponse environment = null;
        if (Objects.nonNull(environmentCrn)) {
            environment = measure(() ->
                            ThreadBasedUserCrnProvider.doAsInternalActor(() ->
                                    environmentClientService.getByCrn(environmentCrn)),
                    LOGGER,
                    "Get Environment from Environment service took {} ms");
        }
        return environment;
    }

    private Long getFirstValidPrivateId(List<InstanceGroup> instanceGroups) {
        LOGGER.debug("Get first valid PrivateId of instanceGroups");
        long id = instanceGroups.stream()
                .flatMap(ig -> ig.getAllInstanceMetaData().stream())
                .filter(im -> im.getPrivateId() != null)
                .map(InstanceMetaData::getPrivateId)
                .map(i -> i + 1)
                .max(Long::compare)
                .orElse(0L);
        LOGGER.debug("First valid privateId: {}", id);
        return id;
    }
}
