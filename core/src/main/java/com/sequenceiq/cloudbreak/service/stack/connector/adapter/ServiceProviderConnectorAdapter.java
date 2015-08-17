package com.sequenceiq.cloudbreak.service.stack.connector.adapter;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.GetSSHFingerprintsRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.GetSSHFingerprintsResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.StartInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.StartInstancesResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopInstancesResult;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpdateStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpdateStackResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpscaleStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpscaleStackResult;
import com.sequenceiq.cloudbreak.cloud.event.setup.SshUserRequest;
import com.sequenceiq.cloudbreak.cloud.event.setup.SshUserResponse;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.service.stack.connector.OperationException;
import com.sequenceiq.cloudbreak.service.stack.flow.ProvisioningService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class ServiceProviderConnectorAdapter implements CloudPlatformConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceProviderConnectorAdapter.class);

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

    @Override
    public Set<Resource> buildStack(Stack stack, String gateWayUserData, String coreUserData, Map<String, Object> setupProperties) {
        LOGGER.info("Assembling launch request for stack: {}", stack);
        CloudContext cloudContext = new CloudContext(stack);
        CloudCredential cloudCredential = credentialConverter.convert(stack.getCredential());
        CloudStack cloudStack = cloudStackConverter.convert(stack, coreUserData, gateWayUserData);
        LaunchStackRequest launchRequest = new LaunchStackRequest(cloudContext, cloudCredential, cloudStack);
        LOGGER.info("Triggering event: {}", launchRequest);
        eventBus.notify(launchRequest.selector(), Event.wrap(launchRequest));
        try {
            LaunchStackResult res = launchRequest.await();
            LOGGER.info("Result: {}", res);
            if (res.isFailed()) {
                if (res.getException() != null) {
                    throw new OperationException("Failed to build the stack", cloudContext, res.getException());
                }
                throw new OperationException(format("Failed to build the stack for %s due to: %s", cloudContext, res.getStatusReason()));
            }
            return transformResults(res.getResults(), stack);
        } catch (InterruptedException e) {
            LOGGER.error("Error while launching stack", e);
            throw new OperationException("Unexpected exception occurred during build stack", cloudContext, e);
        }
    }

    @Override
    public void startAll(Stack stack) {
        LOGGER.info("Assembling start request for stack: {}", stack);
        CloudContext cloudContext = new CloudContext(stack);
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
                throw new OperationException("Unexpected exception occurred during stack start", cloudContext, exception);
            } else {
                for (CloudVmInstanceStatus instanceStatus : res.getResults().getResults()) {
                    if (instanceStatus.getStatus().equals(InstanceStatus.FAILED)) {
                        throw new OperationException("Failed to start the following instance: " + instanceStatus.getCloudInstance());
                    }
                }
            }
        } catch (InterruptedException e) {
            LOGGER.error("Error while starting the stack", e);
            throw new OperationException("Unexpected exception occurred during stack start", cloudContext, e);
        }
    }

    @Override
    public void stopAll(Stack stack) {
        LOGGER.info("Assembling stop request for stack: {}", stack);
        CloudContext cloudContext = new CloudContext(stack);
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
                throw new OperationException("Unexpected exception occurred during stack stop", cloudContext, exception);
            } else {
                for (CloudVmInstanceStatus instanceStatus : res.getResults().getResults()) {
                    if (instanceStatus.getStatus().equals(InstanceStatus.FAILED)) {
                        throw new OperationException("Failed to stop the following instance: " + instanceStatus.getCloudInstance());
                    }
                }
            }
        } catch (InterruptedException e) {
            LOGGER.error("Error while stopping the stack", e);
            throw new OperationException("Unexpected exception occurred during stack stop", cloudContext, e);
        }
    }

    @Override
    public Set<Resource> addInstances(Stack stack, String gateWayUserData, String coreUserData, Integer adjustment, String instanceGroup) {
        LOGGER.debug("Assembling upscale stack event for stack: {}", stack);
        CloudContext cloudContext = new CloudContext(stack);
        CloudCredential cloudCredential = credentialConverter.convert(stack.getCredential());
        InstanceGroup group = stack.getInstanceGroupByInstanceGroupName(instanceGroup);
        group.setNodeCount(group.getNodeCount() + adjustment);
        CloudStack cloudStack = cloudStackConverter.convert(stack, coreUserData, gateWayUserData);
        List<CloudResource> resources = cloudResourceConverter.convert(stack.getResources());
        UpscaleStackRequest<UpscaleStackResult> upscaleRequest = new UpscaleStackRequest<>(cloudContext, cloudCredential, cloudStack, resources);
        LOGGER.info("Triggering upscale stack event: {}", upscaleRequest);
        eventBus.notify(upscaleRequest.selector(), Event.wrap(upscaleRequest));
        try {
            UpscaleStackResult res = upscaleRequest.await();
            LOGGER.info("Upscale stack result: {}", res);
            if (res.isFailed()) {
                if (res.getException() != null) {
                    throw new OperationException("Failed to upscale the stack", cloudContext, res.getException());
                }
                throw new OperationException(format("Failed to upscale the stack: %s due to: %s", cloudContext, res.getStatusReason()));
            }
            return transformResults(res.getResults(), stack);
        } catch (InterruptedException e) {
            LOGGER.error("Error while upscaling the stack", e);
            throw new OperationException("Unexpected exception occurred during add new instances", cloudContext, e);
        }
    }

    @Override
    public Set<String> removeInstances(Stack stack, String gateWayUserData, String coreUserData, Set<String> instanceIds, String instanceGroup) {
        LOGGER.debug("Assembling downscale stack event for stack: {}", stack);
        CloudContext cloudContext = new CloudContext(stack);
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
        CloudStack cloudStack = cloudStackConverter.convert(stack, coreUserData, gateWayUserData, instanceIds);
        DownscaleStackRequest<DownscaleStackResult> downscaleRequest = new DownscaleStackRequest<>(cloudContext,
                cloudCredential, cloudStack, resources, instances);
        LOGGER.info("Triggering downscale stack event: {}", downscaleRequest);
        eventBus.notify(downscaleRequest.selector(), Event.wrap(downscaleRequest));
        try {
            DownscaleStackResult res = downscaleRequest.await();
            LOGGER.info("Downscale stack result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                throw new OperationException(res.getStatusReason(), cloudContext, res.getErrorDetails());
            }
            return instanceIds;
        } catch (InterruptedException e) {
            LOGGER.error("Error while downscaling the stack", e);
            throw new OperationException("Unexpected exception occurred during remove instances", cloudContext, e);
        }
    }

    @Override
    public void deleteStack(Stack stack, Credential credential) {
        LOGGER.debug("Assembling terminate stack event for stack: {}", stack);
        CloudContext cloudContext = new CloudContext(stack);
        CloudCredential cloudCredential = credentialConverter.convert(stack.getCredential());
        List<CloudResource> resources = cloudResourceConverter.convert(stack.getResources());
        CloudStack cloudStack = cloudStackConverter.convert(stack, "", "");
        TerminateStackRequest<TerminateStackResult> terminateRequest = new TerminateStackRequest<>(cloudContext, cloudStack, cloudCredential, resources);
        LOGGER.info("Triggering terminate stack event: {}", terminateRequest);
        eventBus.notify(terminateRequest.selector(), Event.wrap(terminateRequest));
        try {
            TerminateStackResult res = terminateRequest.await();
            LOGGER.info("Terminate stack result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                if (res.getErrorDetails() != null) {
                    throw new OperationException("Failed to terminate the stack", cloudContext, res.getErrorDetails());
                }
                throw new OperationException(format("Failed to terminate the stack: %s due to %s", cloudContext, res.getStatusReason()));
            }
        } catch (InterruptedException e) {
            LOGGER.error("Error while terminating the stack", e);
            throw new OperationException("Unexpected exception occurred during stack termination", cloudContext, e);
        }
    }

    @Override
    public void rollback(Stack stack, Set<Resource> resourceSet) {
        LOGGER.info("Rollback the whole stack for {}", stack.getId());
        deleteStack(stack, stack.getCredential());
    }

    @Override
    public void updateAllowedSubnets(Stack stack, String gateWayUserData, String coreUserData) {
        LOGGER.debug("Assembling update subnet event for: {}", stack);
        CloudContext cloudContext = new CloudContext(stack);
        CloudCredential cloudCredential = credentialConverter.convert(stack.getCredential());
        CloudStack cloudStack = cloudStackConverter.convert(stack, coreUserData, gateWayUserData);
        List<CloudResource> resources = cloudResourceConverter.convert(stack.getResources());
        UpdateStackRequest<UpdateStackResult> updateRequest = new UpdateStackRequest<>(cloudContext, cloudCredential, cloudStack, resources);
        eventBus.notify(updateRequest.selector(), Event.wrap(updateRequest));
        try {
            UpdateStackResult res = updateRequest.await();
            LOGGER.info("Update stack result: {}", res);
            if (res.isFailed()) {
                if (res.getException() != null) {
                    throw new OperationException("Failed to update the stack", cloudContext, res.getException());
                }
                throw new OperationException(format("Failed to update the stack: %s due to: %s", cloudContext, res.getStatusReason()));
            }
        } catch (InterruptedException e) {
            LOGGER.error("Error while updating the stack: " + cloudContext, e);
            throw new OperationException("Unexpected exception occurred during the stack updates", cloudContext, e);
        }
    }

    @Override
    public String getSSHUser(Map<String, String> context) {
        CloudContext cloudContext = new CloudContext(null, null, context.get(ProvisioningService.PLATFORM), null);
        SshUserRequest<SshUserResponse> sshUserRequest = new SshUserRequest<>(cloudContext);
        LOGGER.info("Triggering event: {}", sshUserRequest);
        eventBus.notify(CloudPlatformRequest.selector(SshUserRequest.class), Event.wrap(sshUserRequest));
        try {
            SshUserResponse response = sshUserRequest.await();
            LOGGER.info("Result: {}", response);
            return response.getUser();
        } catch (InterruptedException e) {
            LOGGER.error(format("Error while retrieving ssh user for stack: %s", cloudContext), e);
            throw new OperationException("Unexpected exception occurred during retrieving the SSH user", cloudContext, e);
        }
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.ADAPTER;
    }

    @Override
    public Set<String> getSSHFingerprints(Stack stack, String gateway) {
        Set<String> result = new HashSet<>();
        LOGGER.debug("Get SSH fingerprints of gateway instance for stack: {}", stack);
        CloudContext cloudContext = new CloudContext(stack);
        CloudCredential cloudCredential = credentialConverter.convert(stack.getCredential());
        InstanceMetaData gatewayMetaData = stack.getGatewayInstanceGroup().getInstanceMetaData().iterator().next();
        CloudInstance gatewayInstance = metadataConverter.convert(gatewayMetaData);
        GetSSHFingerprintsRequest<GetSSHFingerprintsResult> sSHFingerprintReq = new GetSSHFingerprintsRequest<>(cloudContext, cloudCredential, gatewayInstance);
        LOGGER.info("Triggering GetSSHFingerprintsRequest stack event: {}", sSHFingerprintReq);
        eventBus.notify(sSHFingerprintReq.selector(), Event.wrap(sSHFingerprintReq));
        try {
            GetSSHFingerprintsResult res = sSHFingerprintReq.await();
            LOGGER.info("Get SSH fingerprints of gateway instance for stack result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                throw new OperationException(res.getStatusReason(), cloudContext, res.getErrorDetails());
            }
            result.addAll(res.getSshFingerprints());
        } catch (InterruptedException e) {
            LOGGER.error(format("Failed to get SSH fingerprints of gateway instance stack: %s", cloudContext), e);
            throw new OperationException("Unexpected exception occurred during retrieving SSH fingerprints", cloudContext, e);
        }
        return result;
    }

    private Set<Resource> transformResults(List<CloudResourceStatus> cloudResourceStatuses, Stack stack) {
        Set<Resource> retSet = new HashSet<>();
        for (CloudResourceStatus cloudResourceStatus : cloudResourceStatuses) {
            Resource resource = new Resource(cloudResourceStatus.getCloudResource().getType(), cloudResourceStatus.getCloudResource().getName(), stack);
            retSet.add(resource);
        }
        return retSet;
    }

}
