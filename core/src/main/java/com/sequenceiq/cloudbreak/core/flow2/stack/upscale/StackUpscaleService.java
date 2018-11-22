package com.sequenceiq.cloudbreak.core.flow2.stack.upscale;

import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.model.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.model.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceStatus.CREATED;
import static java.lang.String.format;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.DetailedStackStatus;
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpscaleStackResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.common.type.BillingStatus;
import com.sequenceiq.cloudbreak.common.type.CommonResourceType;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackScalingFlowContext;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.stack.connector.OperationException;
import com.sequenceiq.cloudbreak.service.stack.flow.MetadataSetupService;
import com.sequenceiq.cloudbreak.service.stack.flow.TlsSetupService;
import com.sequenceiq.cloudbreak.service.usages.UsageService;

@Service
public class StackUpscaleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackUpscaleService.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private FlowMessageService flowMessageService;

    @Inject
    private InstanceGroupRepository instanceGroupRepository;

    @Inject
    private MetadataSetupService metadataSetupService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private CloudbreakEventService eventService;

    @Inject
    private StackToCloudStackConverter cloudStackConverter;

    @Inject
    private UsageService usageService;

    @Inject
    private TlsSetupService tlsSetupService;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private TransactionService transactionService;

    public void startAddInstances(Stack stack, Integer scalingAdjustment) {
        String statusReason = format("Adding %s new instance(s) to the infrastructure.", scalingAdjustment);
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.ADDING_NEW_INSTANCES, statusReason);
    }

    public void addInstanceFireEventAndLog(Stack stack, Integer scalingAdjustment) {
        flowMessageService.fireEventAndLog(stack.getId(),
                flowMessageService.message(Msg.STACK_ADDING_INSTANCES, scalingAdjustment),
                UPDATE_IN_PROGRESS.name());
    }

    public void finishAddInstances(StackScalingFlowContext context, UpscaleStackResult payload) {
        LOGGER.info("Upscale stack result: {}", payload);
        List<CloudResourceStatus> results = payload.getResults();
        validateResourceResults(context, payload.getErrorDetails(), results);
        Set<Resource> resourceSet = transformResults(results, context.getStack());
        if (resourceSet.isEmpty()) {
            throw new OperationException("Failed to upscale the cluster since all create request failed. Resource set is empty");
        }
        LOGGER.debug("Adding new instances to the stack is DONE");
    }

    public void extendingMetadata(Stack stack) {
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.EXTENDING_METADATA);
        clusterService.updateClusterStatusByStackId(stack.getId(), UPDATE_IN_PROGRESS);
    }

    public Set<String> finishExtendMetadata(Stack stack, String instanceGroupName, CollectMetadataResult payload) throws TransactionExecutionException {
        return transactionService.required(() -> {
            List<CloudVmMetaDataStatus> coreInstanceMetaData = payload.getResults();
            int newInstances = metadataSetupService.saveInstanceMetaData(stack, coreInstanceMetaData, CREATED);
            Set<String> upscaleCandidateAddresses = new HashSet<>();
            for (CloudVmMetaDataStatus cloudVmMetaDataStatus : coreInstanceMetaData) {
                upscaleCandidateAddresses.add(cloudVmMetaDataStatus.getMetaData().getPrivateIp());
            }
            InstanceGroup instanceGroup = instanceGroupRepository.findOneByGroupNameInStack(stack.getId(), instanceGroupName);
            int nodeCount = instanceGroup.getNodeCount() + newInstances;
            try {
                clusterService.updateClusterStatusByStackIdOutOfTransaction(stack.getId(), AVAILABLE);
            } catch (TransactionExecutionException e) {
                throw e.getCause();
            }
            eventService.fireCloudbreakEvent(stack.getId(), BillingStatus.BILLING_CHANGED.name(),
                    flowMessageService.message(Msg.STACK_METADATA_SETUP_BILLING_CHANGED));
            flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_METADATA_EXTEND, AVAILABLE.name());
            usageService.scaleUsagesForStack(stack.getId(), instanceGroupName, nodeCount);

            return upscaleCandidateAddresses;
        });
    }

    public void setupTls(StackContext context) throws CloudbreakException {
        Stack stack = context.getStack();
        for (InstanceMetaData gwInstance : stack.getGatewayInstanceMetadata()) {
            if (CREATED.equals(gwInstance.getInstanceStatus())) {
                tlsSetupService.setupTls(stack, gwInstance);
            }
        }
    }

    public void bootstrappingNewNodes(Stack stack) {
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.BOOTSTRAPPING_NEW_NODES);
        flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_BOOTSTRAP_NEW_NODES, UPDATE_IN_PROGRESS.name());
    }

    public void extendingHostMetadata(Stack stack) {
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.EXTENDING_HOST_METADATA);
    }

    public void handleStackUpscaleFailure(long stackId, StackFailureEvent payload) {
        LOGGER.error("Exception during the upscale of stack", payload.getException());
        try {
            String errorReason = payload.getException().getMessage();
            stackUpdater.updateStackStatus(stackId, DetailedStackStatus.UPSCALE_FAILED, "Stack update failed. " + errorReason);
            flowMessageService.fireEventAndLog(stackId, Msg.STACK_INFRASTRUCTURE_UPDATE_FAILED, UPDATE_FAILED.name(), errorReason);
        } catch (RuntimeException e) {
            LOGGER.error("Exception during the handling of stack scaling failure: {}", e.getMessage());
        }
    }

    private Set<Resource> transformResults(Iterable<CloudResourceStatus> cloudResourceStatuses, Stack stack) {
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

    private void validateResourceResults(StackScalingFlowContext context, Exception exception, List<CloudResourceStatus> results) {
        if (exception != null) {
            LOGGER.error(format("Failed to upscale stack: %s", context.getCloudContext()), exception);
            throw new OperationException(exception);
        }
        List<CloudResourceStatus> templates = results.stream().filter(result -> CommonResourceType.TEMPLATE == result.getCloudResource().getType()
                .getCommonResourceType()).collect(Collectors.toList());
        if (!templates.isEmpty() && (templates.get(0).isFailed() || templates.get(0).isDeleted())) {
            throw new OperationException(format("Failed to upscale the stack for %s due to: %s", context.getCloudContext(), templates.get(0).getStatusReason()));
        }
    }

    public List<CloudInstance> buildNewInstances(Stack stack, String extendedInstanceGroupName, int count) {
        Optional<InstanceGroup> extendedInstanceGroup = stack.getInstanceGroups().stream()
                .filter(instanceGroup -> extendedInstanceGroupName.equals(instanceGroup.getGroupName()))
                .findAny();
        long privateId = getFirstValidPrivateId(stack.getInstanceGroupsAsList());
        List<CloudInstance> newInstances = new ArrayList<>();
        if (extendedInstanceGroup.isPresent()) {
            for (long i = 0; i < count; i++) {
                newInstances.add(cloudStackConverter.buildInstance(null, extendedInstanceGroup.get().getTemplate(),
                        stack.getStackAuthentication(), extendedInstanceGroupName, privateId++, InstanceStatus.CREATE_REQUESTED));
            }
        }
        return newInstances;
    }

    private Long getFirstValidPrivateId(List<InstanceGroup> instanceGroups) {
        LOGGER.info("Get first valid PrivateId of instanceGroups");
        long highest = 0;
        for (InstanceGroup instanceGroup : instanceGroups) {
            LOGGER.info("Checking of instanceGroup: {}", instanceGroup.getGroupName());
            for (InstanceMetaData metaData : instanceGroup.getAllInstanceMetaData()) {
                Long privateId = metaData.getPrivateId();
                LOGGER.info("InstanceMetaData metaData: privateId: {}, instanceGroupName: {}, instanceId: {}, status: {}",
                        privateId, metaData.getInstanceGroupName(), metaData.getInstanceId(), metaData.getInstanceStatus());
                if (privateId == null) {
                    continue;
                }
                if (privateId > highest) {
                    highest = privateId;
                }
            }
        }
        LOGGER.info("highest privateId: {}", highest);
        return highest == 0 ? 0 : highest + 1;
    }

    public void mountDisksOnNewHosts(Stack stack) {
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.MOUNTING_DISKS_ON_NEW_HOSTS);
        flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_MOUNT_DISKS_ON_NEW_HOSTS, UPDATE_IN_PROGRESS.name());
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.UPSCALE_COMPLETED, "Stack upscale has been finished successfully.");
        flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_UPSCALE_FINISHED, AVAILABLE.name());
    }
}
