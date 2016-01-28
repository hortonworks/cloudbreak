package com.sequenceiq.cloudbreak.service.stack.connector.adapter;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static java.lang.String.format;
import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.StartInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.StartInstancesResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopInstancesResult;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.event.platform.CheckPlatformVariantRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.CheckPlatformVariantResult;
import com.sequenceiq.cloudbreak.cloud.event.platform.PlatformParameterRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.PlatformParameterResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpdateStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpdateStackResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpscaleStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpscaleStackResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetadataService;
import com.sequenceiq.cloudbreak.service.stack.connector.OperationException;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class ServiceProviderConnectorAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceProviderConnectorAdapter.class);
    private static final String ROLLBACK_MESSAGE = "stack.infrastructure.create.rollback";

    @Inject
    private EventBus eventBus;
    @Inject
    private StackToCloudStackConverter cloudStackConverter;
    @Inject
    private InstanceMetaDataToCloudInstanceConverter metadataConverter;
    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;
    @Inject
    private ResourceToCloudResourceConverter cloudResourceConverter;
    @Inject
    private InstanceGroupRepository instanceGroupRepository;
    @Inject
    private CloudbreakEventService eventService;
    @Inject
    private CloudbreakMessagesService cloudbreakMessagesService;
    @Inject
    private InstanceMetadataService instanceMetadataService;

    public void startAll(Stack stack) {
        LOGGER.info("Assembling start request for stack: {}", stack);
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), stack.cloudPlatform(), stack.getOwner(), stack.getPlatformVariant(),
                location);
        CloudCredential cloudCredential = credentialConverter.convert(stack.getCredential());
        List<CloudInstance> instances = metadataConverter.convert(stack.getInstanceMetaDataAsList());
        List<CloudResource> resources = cloudResourceConverter.convert(stack.getResources());
        StartInstancesRequest startRequest = new StartInstancesRequest(cloudContext, cloudCredential, resources, instances);
        LOGGER.info("Triggering event: {}", startRequest);
        eventBus.notify(startRequest.selector(), Event.wrap(startRequest));
        try {
            StartInstancesResult res = startRequest.await();
            LOGGER.info("Result: {}", res);
            if (res.isFailed()) {
                Exception exception = res.getException();
                LOGGER.error(format("Failed to start the stack: %s", cloudContext), exception);
                throw new OperationException(exception);
            } else {
                for (CloudVmInstanceStatus instanceStatus : res.getResults().getResults()) {
                    if (instanceStatus.getStatus().equals(InstanceStatus.FAILED)) {
                        throw new OperationException("Failed to start the following instance: " + instanceStatus.getCloudInstance());
                    }
                }
            }
        } catch (InterruptedException e) {
            LOGGER.error("Error while starting the stack", e);
            throw new OperationException(e);
        }
    }

    public void stopAll(Stack stack) {
        LOGGER.info("Assembling stop request for stack: {}", stack);
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), stack.cloudPlatform(), stack.getOwner(), stack.getPlatformVariant(),
                location);
        CloudCredential cloudCredential = credentialConverter.convert(stack.getCredential());
        List<CloudInstance> instances = metadataConverter.convert(stack.getInstanceMetaDataAsList());
        List<CloudResource> resources = cloudResourceConverter.convert(stack.getResources());
        StopInstancesRequest<StopInstancesResult> stopRequest = new StopInstancesRequest<>(cloudContext, cloudCredential, resources, instances);
        LOGGER.info("Triggering event: {}", stopRequest);
        eventBus.notify(stopRequest.selector(), Event.wrap(stopRequest));
        try {
            StopInstancesResult res = stopRequest.await();
            LOGGER.info("Result: {}", res);
            if (res.isFailed()) {
                Exception exception = res.getException();
                LOGGER.error("Failed to stop the stack", exception);
                throw new OperationException(exception);
            } else {
                for (CloudVmInstanceStatus instanceStatus : res.getResults().getResults()) {
                    if (instanceStatus.getStatus().equals(InstanceStatus.FAILED)) {
                        throw new OperationException("Failed to stop the following instance: " + instanceStatus.getCloudInstance());
                    }
                }
            }
        } catch (InterruptedException e) {
            LOGGER.error("Error while stopping the stack", e);
            throw new OperationException(e);
        }
    }

    public Set<Resource> addInstances(Stack stack, Integer adjustment, String instanceGroup) {
        LOGGER.debug("Assembling upscale stack event for stack: {}", stack);
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), stack.cloudPlatform(), stack.getOwner(), stack.getPlatformVariant(),
                location);
        CloudCredential cloudCredential = credentialConverter.convert(stack.getCredential());
        InstanceGroup group = stack.getInstanceGroupByInstanceGroupName(instanceGroup);
        group.setNodeCount(group.getNodeCount() + adjustment);
        CloudStack cloudStack = cloudStackConverter.convert(stack);
        instanceMetadataService.saveInstanceRequests(stack, cloudStack.getGroups());
        List<CloudResource> resources = cloudResourceConverter.convert(stack.getResources());
        UpscaleStackRequest<UpscaleStackResult> upscaleRequest = new UpscaleStackRequest<>(cloudContext, cloudCredential, cloudStack, resources);
        LOGGER.info("Triggering upscale stack event: {}", upscaleRequest);
        eventBus.notify(upscaleRequest.selector(), Event.wrap(upscaleRequest));
        try {
            UpscaleStackResult res = upscaleRequest.await();
            LOGGER.info("Upscale stack result: {}", res);
            List<CloudResourceStatus> results = res.getResults();
            validateResourceResults(cloudContext, res);
            updateNodeCount(stack.getId(), cloudStack.getGroups(), results, false);
            Set<Resource> resourceSet = transformResults(results, stack);
            if (resourceSet.isEmpty()) {
                throw new OperationException("Failed to upscale the cluster since all create request failed: " + results.get(0).getStatusReason());
            }
            return resourceSet;
        } catch (InterruptedException e) {
            LOGGER.error("Error while upscaling the stack", e);
            throw new OperationException(e);
        }
    }

    public Set<String> removeInstances(Stack stack, Set<String> instanceIds, String instanceGroup) {
        LOGGER.debug("Assembling downscale stack event for stack: {}", stack);
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), stack.cloudPlatform(), stack.getOwner(), stack.getPlatformVariant(),
                location);
        CloudCredential cloudCredential = credentialConverter.convert(stack.getCredential());
        List<CloudResource> resources = cloudResourceConverter.convert(stack.getResources());
        List<CloudInstance> instances = new ArrayList<>();
        InstanceGroup group = stack.getInstanceGroupByInstanceGroupName(instanceGroup);
        for (InstanceMetaData metaData : group.getAllInstanceMetaData()) {
            if (instanceIds.contains(metaData.getInstanceId())) {
                CloudInstance cloudInstance = metadataConverter.convert(metaData);
                instances.add(cloudInstance);
            }
        }
        CloudStack cloudStack = cloudStackConverter.convert(stack, instanceIds);
        DownscaleStackRequest<DownscaleStackResult> downscaleRequest = new DownscaleStackRequest<>(cloudContext,
                cloudCredential, cloudStack, resources, instances);
        LOGGER.info("Triggering downscale stack event: {}", downscaleRequest);
        eventBus.notify(downscaleRequest.selector(), Event.wrap(downscaleRequest));
        try {
            DownscaleStackResult res = downscaleRequest.await();
            LOGGER.info("Downscale stack result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                LOGGER.error("Failed to downscale the stack", res.getErrorDetails());
                throw new OperationException(res.getErrorDetails());
            }
            return instanceIds;
        } catch (InterruptedException e) {
            LOGGER.error("Error while downscaling the stack", e);
            throw new OperationException(e);
        }
    }

    public void deleteStack(Stack stack, Credential credential) {
        LOGGER.debug("Assembling terminate stack event for stack: {}", stack);
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), stack.cloudPlatform(), stack.getOwner(), stack.getPlatformVariant(),
                location);
        CloudCredential cloudCredential = credentialConverter.convert(stack.getCredential());
        List<CloudResource> resources = cloudResourceConverter.convert(stack.getResources());
        CloudStack cloudStack = cloudStackConverter.convert(stack);
        TerminateStackRequest<TerminateStackResult> terminateRequest = new TerminateStackRequest<>(cloudContext, cloudStack, cloudCredential, resources);
        LOGGER.info("Triggering terminate stack event: {}", terminateRequest);
        eventBus.notify(terminateRequest.selector(), Event.wrap(terminateRequest));
        try {
            TerminateStackResult res = terminateRequest.await();
            LOGGER.info("Terminate stack result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                if (res.getErrorDetails() != null) {
                    LOGGER.error("Failed to terminate the stack", res.getErrorDetails());
                    throw new OperationException(res.getErrorDetails());
                }
                throw new OperationException(format("Failed to terminate the stack: %s due to %s", cloudContext, res.getStatusReason()));
            }
        } catch (InterruptedException e) {
            LOGGER.error("Error while terminating the stack", e);
            throw new OperationException(e);
        }
    }

    public void rollback(Stack stack, Set<Resource> resourceSet) {
        LOGGER.info("Rollback the whole stack for {}", stack.getId());
        deleteStack(stack, stack.getCredential());
    }

    public void updateAllowedSubnets(Stack stack) {
        LOGGER.debug("Assembling update subnet event for: {}", stack);
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), stack.cloudPlatform(), stack.getOwner(), stack.getPlatformVariant(),
                location);
        CloudCredential cloudCredential = credentialConverter.convert(stack.getCredential());
        CloudStack cloudStack = cloudStackConverter.convert(stack);
        List<CloudResource> resources = cloudResourceConverter.convert(stack.getResources());
        UpdateStackRequest<UpdateStackResult> updateRequest = new UpdateStackRequest<>(cloudContext, cloudCredential, cloudStack, resources);
        eventBus.notify(updateRequest.selector(), Event.wrap(updateRequest));
        try {
            UpdateStackResult res = updateRequest.await();
            LOGGER.info("Update stack result: {}", res);
            if (res.isFailed()) {
                if (res.getException() != null) {
                    LOGGER.error("Failed to update the stack", res.getException());
                    throw new OperationException(res.getException());
                }
                throw new OperationException(format("Failed to update the stack: %s due to: %s", cloudContext, res.getStatusReason()));
            }
        } catch (InterruptedException e) {
            LOGGER.error("Error while updating the stack: " + cloudContext, e);
            throw new OperationException(e);
        }
    }

    public PlatformParameters getPlatformParameters(Stack stack) {
        LOGGER.debug("Get platform parameters for: {}", stack);
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), stack.cloudPlatform(), stack.getOwner(), stack.getPlatformVariant(),
                location);
        CloudCredential cloudCredential = credentialConverter.convert(stack.getCredential());
        PlatformParameterRequest parameterRequest = new PlatformParameterRequest(cloudContext, cloudCredential);
        eventBus.notify(parameterRequest.selector(), Event.wrap(parameterRequest));
        try {
            PlatformParameterResult res = parameterRequest.await();
            LOGGER.info("Platform parameter result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                LOGGER.error("Failed to get platform parameters", res.getErrorDetails());
                throw new OperationException(res.getErrorDetails());
            }
            return res.getPlatformParameters();
        } catch (InterruptedException e) {
            LOGGER.error("Error while getting platform parameters: " + cloudContext, e);
            throw new OperationException(e);
        }
    }

    public Variant checkAndGetPlatformVariant(Stack stack) {
        LOGGER.debug("Get platform variant for: {}", stack);
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), stack.cloudPlatform(), stack.getOwner(), stack.getPlatformVariant(),
                location);
        CloudCredential cloudCredential = credentialConverter.convert(stack.getCredential());
        CheckPlatformVariantRequest checkPlatformVariantRequest = new CheckPlatformVariantRequest(cloudContext, cloudCredential);
        eventBus.notify(checkPlatformVariantRequest.selector(), Event.wrap(checkPlatformVariantRequest));
        try {
            CheckPlatformVariantResult res = checkPlatformVariantRequest.await();
            LOGGER.info("Platform variant result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                LOGGER.error("Failed to get platform variant", res.getErrorDetails());
                throw new OperationException(res.getErrorDetails());
            }
            return res.getDefaultPlatformVariant();
        } catch (InterruptedException e) {
            LOGGER.error("Error while getting the platform variant: " + cloudContext, e);
            throw new OperationException(e);
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

    private void validateResourceResults(CloudContext cloudContext, LaunchStackResult res) {
        validateResourceResults(cloudContext, res.getErrorDetails(), res.getResults(), true);
    }

    private void validateResourceResults(CloudContext cloudContext, UpscaleStackResult res) {
        validateResourceResults(cloudContext, res.getException(), res.getResults(), false);
    }

    private void validateResourceResults(CloudContext cloudContext, Exception exception, List<CloudResourceStatus> results, boolean create) {
        String action = create ? "create" : "upscale";
        if (exception != null) {
            LOGGER.error(format("Failed to %s stack: %s", action, cloudContext), exception);
            throw new OperationException(exception);
        }
        if (results.size() == 1 && (results.get(0).isFailed() || results.get(0).isDeleted())) {
            throw new OperationException(format("Failed to %s the stack for %s due to: %s", action, cloudContext, results.get(0).getStatusReason()));
        }
    }

    private void updateNodeCount(long stackId, List<Group> originalGroups, List<CloudResourceStatus> statuses, boolean create) {
        for (Group group : originalGroups) {
            int nodeCount = group.getInstances().size();
            List<CloudResourceStatus> failedResources = removeFailedMetadata(stackId, statuses, group);
            if (!failedResources.isEmpty() && create) {
                int failedCount = failedResources.size();
                InstanceGroup instanceGroup = instanceGroupRepository.findOneByGroupNameInStack(stackId, group.getName());
                instanceGroup.setNodeCount(nodeCount - failedCount);
                instanceGroupRepository.save(instanceGroup);
                String message = cloudbreakMessagesService.getMessage(ROLLBACK_MESSAGE,
                        asList(failedCount, group.getName(), failedResources.get(0).getStatusReason()));
                eventService.fireCloudbreakEvent(stackId, Status.UPDATE_IN_PROGRESS.name(), message);
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

}
