package com.sequenceiq.cloudbreak.service.stack.connector.adapter;

import static com.sequenceiq.cloudbreak.service.stack.flow.ReflectionUtils.getDeclaredFields;
import static java.lang.String.format;
import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.jasypt.encryption.pbe.PBEStringCleanablePasswordEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.GetSSHFingerprintsRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.GetSSHFingerprintsResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.StartInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.StartInstancesResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopInstancesResult;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.GetInstancesStateRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.GetInstancesStateResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpdateStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpdateStackResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpscaleStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpscaleStackResult;
import com.sequenceiq.cloudbreak.cloud.event.setup.PreProvisionCheckRequest;
import com.sequenceiq.cloudbreak.cloud.event.setup.PreProvisionCheckResult;
import com.sequenceiq.cloudbreak.cloud.event.setup.SetupRequest;
import com.sequenceiq.cloudbreak.cloud.event.setup.SetupResult;
import com.sequenceiq.cloudbreak.cloud.event.setup.SshUserRequest;
import com.sequenceiq.cloudbreak.cloud.event.setup.SshUserResponse;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.converter.spi.CloudVmInstanceStatusToCoreInstanceMetaDataConverter;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.SecurityRuleRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.service.stack.connector.MetadataSetup;
import com.sequenceiq.cloudbreak.service.stack.connector.ProvisionSetup;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionEvent;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionSetupComplete;
import com.sequenceiq.cloudbreak.service.stack.flow.CoreInstanceMetaData;
import com.sequenceiq.cloudbreak.service.stack.flow.InstanceSyncState;
import com.sequenceiq.cloudbreak.service.stack.flow.ProvisioningService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class ServiceProviderAdapter implements ProvisionSetup, MetadataSetup, CloudPlatformConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceProviderAdapter.class);

    @Inject
    private EventBus eventBus;
    @Inject
    private StackRepository stackRepository;
    @Inject
    private SecurityRuleRepository securityRuleRepository;
    @Inject
    private PBEStringCleanablePasswordEncryptor pbeStringCleanablePasswordEncryptor;
    @Inject
    private StackToCloudStackConverter cloudStackConverter;
    @Inject
    private InstanceMetaDataToCloudInstanceConverter metadataConverter;
    @Inject
    private CloudVmInstanceStatusToCoreInstanceMetaDataConverter instanceConverter;
    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;
    @Inject
    private ResourceToCloudResourceConverter cloudResourceConverter;

    @Override
    public String preProvisionCheck(Stack stack) {
        CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), stack.cloudPlatform().name(), stack.getOwner());
        CloudCredential cloudCredential = credentialConverter.convert(stack.getCredential());
        CloudStack cloudStack = cloudStackConverter.convert(stack);
        PreProvisionCheckRequest<PreProvisionCheckResult> preProvisionCheckRequest = new PreProvisionCheckRequest<>(cloudContext, cloudCredential, cloudStack);
        LOGGER.info("Triggering event: {}", preProvisionCheckRequest);
        eventBus.notify(preProvisionCheckRequest.selector(), Event.wrap(preProvisionCheckRequest));
        try {
            PreProvisionCheckResult res = preProvisionCheckRequest.await();
            LOGGER.info("Result: {}", res);
            if (res.getErrorDetails() != null) {
                return res.getErrorDetails().getMessage();
            }
            return res.getStatusReason();
        } catch (InterruptedException e) {
            LOGGER.error("Error while executing pre-provision check", e);
            return e.getMessage();
        }
    }

    @Override
    public ProvisionEvent setupProvisioning(Stack stack) throws Exception {
        CloudPlatform cloudPlatform = stack.cloudPlatform();
        CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), cloudPlatform.name(), stack.getOwner());
        CloudCredential cloudCredential = credentialConverter.convert(stack.getCredential());
        CloudStack cloudStack = cloudStackConverter.convert(stack);
        SetupRequest<SetupResult> setupRequest = new SetupRequest<>(cloudContext, cloudCredential, cloudStack);
        LOGGER.info("Triggering event: {}", setupRequest);
        eventBus.notify(setupRequest.selector(), Event.wrap(setupRequest));
        try {
            SetupResult res = setupRequest.await();
            LOGGER.info("Result: {}", res);
            if (res.getErrorDetails() != null) {
                throw new OperationException("Failed to setup provisioning", cloudContext, res.getErrorDetails());
            }
            return new ProvisionSetupComplete(cloudPlatform, stack.getId()).withSetupProperties(res.getSetupProperties());
        } catch (InterruptedException e) {
            LOGGER.error("Error while executing provisioning setup", e);
            throw new OperationException("Unexpected exception occurred during provisioning setup", cloudContext, e);
        }
    }

    @Override
    public Set<Resource> buildStack(Stack stack, String gateWayUserData, String coreUserData, Map<String, Object> setupProperties) {
        LOGGER.info("Assembling launch request for stack: {}", stack);
        CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), stack.cloudPlatform().name(), stack.getOwner());
        CloudCredential cloudCredential = buildCloudCredential(stack);
        CloudStack cloudStack = cloudStackConverter.convert(stack, coreUserData, gateWayUserData);

        LaunchStackRequest<LaunchStackResult> launchRequest = new LaunchStackRequest<>(cloudContext, cloudCredential, cloudStack);
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
        CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), stack.cloudPlatform().name(), stack.getOwner());
        CloudCredential cloudCredential = buildCloudCredential(stack);
        List<CloudInstance> instances = metadataConverter.convert(stack.getInstanceMetaDataAsList());
        StartInstancesRequest<StartInstancesResult> startRequest = new StartInstancesRequest<>(cloudContext, cloudCredential, instances);
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
        CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), stack.cloudPlatform().name(), stack.getOwner());
        CloudCredential cloudCredential = buildCloudCredential(stack);
        List<CloudInstance> instances = metadataConverter.convert(stack.getInstanceMetaDataAsList());
        StopInstancesRequest<StopInstancesResult> stopRequest = new StopInstancesRequest<>(cloudContext, cloudCredential, instances);
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
        CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), stack.cloudPlatform().name(), stack.getOwner());
        CloudCredential cloudCredential = buildCloudCredential(stack);
        InstanceGroup group = stack.getInstanceGroupByInstanceGroupName(instanceGroup);
        group.setNodeCount(group.getNodeCount() + adjustment);
        CloudStack cloudStack = cloudStackConverter.convert(stack, coreUserData, gateWayUserData);
        //TODO which resources?
        List<CloudResource> resources = cloudResourceConverter.convert(stack.getResources());
        UpscaleStackRequest<UpscaleStackResult> upscaleRequest = new UpscaleStackRequest<>(cloudContext, cloudCredential, cloudStack, resources, adjustment);
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
        CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), stack.cloudPlatform().name(), stack.getOwner());
        CloudCredential cloudCredential = buildCloudCredential(stack);
        List<CloudResource> resources = cloudResourceConverter.convert(stack.getResources());
        List<InstanceTemplate> instanceTemplates = new ArrayList<>();
        InstanceGroup group = stack.getInstanceGroupByInstanceGroupName(instanceGroup);
        List<InstanceMetaData> temporarilyRemovedInstances = new ArrayList<>();
        for (InstanceMetaData metaData : group.getAllInstanceMetaData()) {
            if (instanceIds.contains(metaData.getInstanceId())) {
                CloudInstance cloudInstance = metadataConverter.convert(metaData);
                instanceTemplates.add(cloudInstance.getTemplate());
                metaData.setInstanceStatus(com.sequenceiq.cloudbreak.domain.InstanceStatus.TERMINATED);
                temporarilyRemovedInstances.add(metaData);
            }
        }
        Integer originalGroupNodeCount = group.getNodeCount();
        //hacking node count for the appropriate CloudStack conversion
        group.setNodeCount(originalGroupNodeCount - instanceIds.size());
        CloudStack cloudStack = cloudStackConverter.convert(stack, coreUserData, gateWayUserData);
        //set back original node count
        group.setNodeCount(originalGroupNodeCount);
        for (InstanceMetaData metaData : temporarilyRemovedInstances) {
            metaData.setInstanceStatus(com.sequenceiq.cloudbreak.domain.InstanceStatus.UNREGISTERED);
        }
        DownscaleStackRequest<DownscaleStackResult> downscaleRequest = new DownscaleStackRequest<>(cloudContext,
                cloudCredential, cloudStack, resources, instanceTemplates);
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
        CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), stack.cloudPlatform().name(), stack.getOwner());
        CloudCredential cloudCredential = buildCloudCredential(stack);
        //TODO which resources?
        List<CloudResource> resources = cloudResourceConverter.convert(stack.getResources());
        CloudStack cloudStack = cloudStackConverter.convert(stack, "", "");
        TerminateStackRequest<TerminateStackResult> terminateRequest = new TerminateStackRequest<>(cloudContext, cloudStack, cloudCredential, resources);
        LOGGER.info("Triggering terminate stack event: {}", terminateRequest);
        eventBus.notify(terminateRequest.selector(), Event.wrap(terminateRequest));
        try {
            TerminateStackResult res = terminateRequest.await();
            LOGGER.info("Terminate stack result: {}", res);
            //TODO shouldn't we allow cluster delete then, what if someone deletes the stack by hand?
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
        CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), stack.cloudPlatform().name(), stack.getOwner());
        CloudCredential cloudCredential = buildCloudCredential(stack);
        CloudStack cloudStack = cloudStackConverter.convert(stack, coreUserData, gateWayUserData);
        //TODO which resources?
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
    public Set<CoreInstanceMetaData> collectMetadata(Stack stack) {
        CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), stack.cloudPlatform().name(), stack.getOwner());
        CloudCredential cloudCredential = credentialConverter.convert(stack.getCredential());
        List<InstanceTemplate> instanceTemplates = cloudStackConverter.buildInstanceTemplates(stack);
        List<CloudResource> cloudResources = cloudResourceConverter.convert(stack.getResources());
        CollectMetadataRequest<CollectMetadataResult> cmr = new CollectMetadataRequest<>(cloudContext, cloudCredential, cloudResources, instanceTemplates);
        LOGGER.info("Triggering event: {}", cmr);
        eventBus.notify(cmr.selector(CollectMetadataRequest.class), Event.wrap(cmr));
        try {
            CollectMetadataResult res = cmr.await();
            LOGGER.info("Result: {}", res);
            if (res.getException() != null) {
                LOGGER.error("Failed to collect metadata", res.getException());
                return Collections.emptySet();
            }
            return new HashSet<>(instanceConverter.convert(res.getResults()));
        } catch (InterruptedException e) {
            LOGGER.error(format("Error while executing collectMetadata, stack: %s", cloudContext), e);
            throw new OperationException("Unexpected exception occurred during collecting the metadata", cloudContext, e);
        }
    }

    @Override
    public Set<CoreInstanceMetaData> collectNewMetadata(Stack stack, Set<Resource> resourceList, String instanceGroupName, Integer scalingAdjustment) {
        CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), stack.cloudPlatform().name(), stack.getOwner());
        CloudCredential cloudCredential = credentialConverter.convert(stack.getCredential());
        List<InstanceTemplate> instanceTemplates = getNewInstanceTemplates(stack, instanceGroupName, scalingAdjustment);
        List<CloudResource> cloudResources = cloudResourceConverter.convert(stack.getResources());
        CollectMetadataRequest<CollectMetadataResult> cmr = new CollectMetadataRequest<>(cloudContext, cloudCredential, cloudResources, instanceTemplates);
        LOGGER.info("Triggering event: {}", cmr);
        eventBus.notify(cmr.selector(CollectMetadataRequest.class), Event.wrap(cmr));
        try {
            CollectMetadataResult res = cmr.await();
            LOGGER.info("Result: {}", res);
            if (res.getException() != null) {
                LOGGER.error(format("Failed to collect metadata, stack: %s", cloudContext), res.getException());
                return Collections.emptySet();
            }
            return new HashSet<>(instanceConverter.convert(res.getResults()));
        } catch (InterruptedException e) {
            LOGGER.error(format("Error while collecting new metadata for stack: %s", cloudContext), e);
            throw new OperationException("Unexpected exception occurred during collecting the metadata", cloudContext, e);
        }
    }

    @Override
    public InstanceSyncState getState(Stack stack, InstanceGroup instanceGroup, String instanceId) {
        CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), stack.cloudPlatform().name(), stack.getOwner());
        CloudCredential cloudCredential = credentialConverter.convert(stack.getCredential());
        InstanceGroup ig = stack.getInstanceGroupByInstanceGroupName(instanceGroup.getGroupName());
        CloudInstance instance = null;
        for (InstanceMetaData metaData : ig.getAllInstanceMetaData()) {
            if (instanceId.equalsIgnoreCase(metaData.getInstanceId())) {
                instance = metadataConverter.convert(metaData);
                break;
            }
        }
        if (instance != null) {
            GetInstancesStateRequest<GetInstancesStateResult> stateRequest =
                    new GetInstancesStateRequest<>(cloudContext, cloudCredential, asList(instance));
            LOGGER.info("Triggering event: {}", stateRequest);
            eventBus.notify(stateRequest.selector(), Event.wrap(stateRequest));
            try {
                GetInstancesStateResult res = stateRequest.await();
                LOGGER.info("Result: {}", res);
                if (res.isFailed()) {
                    throw new OperationException("Failed to retrieve instance state", cloudContext, res.getException());
                }
                return transform(res.getStatuses().get(0).getStatus());
            } catch (InterruptedException e) {
                LOGGER.error(format("Error while retrieving instance state of: %s", cloudContext), e);
                throw new OperationException("Unexpected exception occurred during instance state retrieval", cloudContext, e);
            }
        } else {
            return InstanceSyncState.DELETED;
        }
    }

    @Override
    public ResourceType getInstanceResourceType() {
        return null;
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.ADAPTER;
    }

    @Override
    public Set<String> getSSHFingerprints(Stack stack, String gateway) {
        Set<String> result = new HashSet<>();
        LOGGER.debug("Get SSH fingerprints of gateway instance for stack: {}", stack);
        CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), stack.cloudPlatform().name(), stack.getOwner());
        CloudCredential cloudCredential = buildCloudCredential(stack);
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

    private CloudCredential buildCloudCredential(Stack stack) {
        Credential credential = stack.getCredential();
        return new CloudCredential(credential.getName(), getDeclaredFields(credential));
    }

    private Set<Resource> transformResults(List<CloudResourceStatus> cloudResourceStatuses, Stack stack) {
        Set<Resource> retSet = new HashSet<>();
        for (CloudResourceStatus cloudResourceStatus : cloudResourceStatuses) {
            Resource resource = new Resource(cloudResourceStatus.getCloudResource().getType(), cloudResourceStatus.getCloudResource().getReference(), stack);
            retSet.add(resource);
        }
        return retSet;
    }

    private List<InstanceTemplate> getNewInstanceTemplates(Stack stack, String instanceGroupName, Integer scalingAdjustment) {
        List<Long> existingIds = new ArrayList<>();
        for (InstanceMetaData data : stack.getInstanceMetaDataAsList()) {
            existingIds.add(data.getPrivateId());
        }
        InstanceGroup group = stack.getInstanceGroupByInstanceGroupName(instanceGroupName);
        group.setNodeCount(group.getNodeCount() + scalingAdjustment);
        List<InstanceTemplate> instanceTemplates = cloudStackConverter.buildInstanceTemplates(stack);
        Iterator<InstanceTemplate> iterator = instanceTemplates.iterator();
        while (iterator.hasNext()) {
            if (existingIds.contains(iterator.next().getPrivateId())) {
                iterator.remove();
            }
        }
        return instanceTemplates;
    }

    private InstanceSyncState transform(InstanceStatus instanceStatus) {
        switch (instanceStatus) {
            case IN_PROGRESS:
                return InstanceSyncState.IN_PROGRESS;
            case STARTED:
                return InstanceSyncState.RUNNING;
            case STOPPED:
                return InstanceSyncState.STOPPED;
            case CREATED:
                return InstanceSyncState.RUNNING;
            case FAILED:
                return InstanceSyncState.DELETED;
            case TERMINATED:
                return InstanceSyncState.DELETED;
            default:
                return InstanceSyncState.UNKNOWN;
        }
    }

}
