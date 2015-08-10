package com.sequenceiq.cloudbreak.service.stack.connector;

import static com.sequenceiq.cloudbreak.service.stack.flow.ReflectionUtils.getDeclaredFields;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackResult;
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
import com.sequenceiq.cloudbreak.domain.OpenStackCredential;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.SecurityRuleRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.stack.connector.openstack.OpenStackResourceException;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionEvent;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionSetupComplete;
import com.sequenceiq.cloudbreak.service.stack.flow.CoreInstanceMetaData;
import com.sequenceiq.cloudbreak.service.stack.flow.InstanceSyncState;
import com.sequenceiq.cloudbreak.service.stack.flow.ProvisioningService;

import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.rx.Promise;
import reactor.rx.Promises;

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
        CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), stack.cloudPlatform().name());
        CloudCredential cloudCredential = credentialConverter.convert(stack.getCredential());
        CloudStack cloudStack = cloudStackConverter.convert(stack);
        Promise<PreProvisionCheckResult> promise = Promises.prepare();
        PreProvisionCheckRequest preProvisionCheckRequest = new PreProvisionCheckRequest(cloudContext, cloudCredential, cloudStack, promise);
        LOGGER.info("Triggering event: {}", preProvisionCheckRequest);
        eventBus.notify(preProvisionCheckRequest.selector(), Event.wrap(preProvisionCheckRequest));
        PreProvisionCheckResult res;
        try {
            res = promise.await(1, TimeUnit.HOURS);
            LOGGER.info("Result: {}", res);
        } catch (InterruptedException e) {
            LOGGER.error("Error while executing pre-provision check", e);
            return e.getMessage();
        }
        return res.getStatusReason();
    }

    @Override
    public ProvisionEvent setupProvisioning(Stack stack) throws Exception {
        CloudPlatform cloudPlatform = stack.cloudPlatform();
        CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), cloudPlatform.name());
        CloudCredential cloudCredential = credentialConverter.convert(stack.getCredential());
        CloudStack cloudStack = cloudStackConverter.convert(stack);
        Promise<SetupResult> promise = Promises.prepare();
        SetupRequest setupRequest = new SetupRequest(cloudContext, cloudCredential, cloudStack, promise);
        LOGGER.info("Triggering event: {}", setupRequest);
        eventBus.notify(setupRequest.selector(), Event.wrap(setupRequest));
        SetupResult res;
        try {
            res = promise.await(1, TimeUnit.HOURS);
            LOGGER.info("Result: {}", res);
            res.check();
        } catch (InterruptedException e) {
            LOGGER.error("Error while executing pre-provision check", e);
        }
        return new ProvisionSetupComplete(cloudPlatform, stack.getId());
    }

    @Override
    public Set<Resource> buildStack(Stack stack, String gateWayUserData, String coreUserData, Map<String, Object> setupProperties) {
        LOGGER.info("Assembling launch request for stack: {}", stack);
        LaunchStackResult res = null;

        CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), stack.cloudPlatform().name());
        CloudCredential cloudCredential = buildCloudCredential(stack);
        CloudStack cloudStack = cloudStackConverter.convert(stack, coreUserData, gateWayUserData);

        Promise<LaunchStackResult> promise = Promises.prepare();
        LaunchStackRequest launchStackRequest = new LaunchStackRequest(cloudContext, cloudCredential, cloudStack, promise);
        LOGGER.info("Triggering event: {}", launchStackRequest);
        eventBus.notify(launchStackRequest.selector(), Event.wrap(launchStackRequest));
        try {
            res = promise.await(1, TimeUnit.HOURS);
            LOGGER.info("Result: {}", res);
            if (res == null) {
                throw new OpenStackResourceException("Launch of stack failed: resource(s) could not be created in time.");
            } else if (res.getStatus().equals(EventStatus.FAILED)) {
                throw new OpenStackResourceException(res.getStatusReason());
            }
        } catch (InterruptedException e) {
            LOGGER.error("Error while launching stack: ", e);
        }
        return transformResults(res.getResults(), stack);
    }

    @Override
    public void startAll(Stack stack) {
        LOGGER.info("Assembling start request for stack: {}", stack);
        CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), stack.cloudPlatform().name());
        CloudCredential cloudCredential = buildCloudCredential(stack);
        List<CloudInstance> instances = metadataConverter.convert(stack.getInstanceMetaDataAsList());
        Promise<StartInstancesResult> promise = Promises.prepare();
        StartInstancesRequest startStackRequest = new StartInstancesRequest(cloudContext, cloudCredential, instances, promise);
        //TODO we should create the promise inside the request to avoid ClassCastExceptions
        //TODO the request should know what type of result will come back
        LOGGER.info("Triggering event: {}", startStackRequest);
        eventBus.notify(startStackRequest.selector(), Event.wrap(startStackRequest));
        try {
            StartInstancesResult res = promise.await(1, TimeUnit.HOURS);
            LOGGER.info("Result: {}", res);
        } catch (InterruptedException e) {
            LOGGER.error("Error while starting stack: ", e);
        }
    }

    @Override
    public void stopAll(Stack stack) {
        LOGGER.info("Assembling stop request for stack: {}", stack);
        CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), stack.cloudPlatform().name());
        CloudCredential cloudCredential = buildCloudCredential(stack);
        List<CloudInstance> instances = metadataConverter.convert(stack.getInstanceMetaDataAsList());
        Promise<StopInstancesResult> promise = Promises.prepare();
        StopInstancesRequest stopStackRequest = new StopInstancesRequest(cloudContext, cloudCredential, instances, promise);
        LOGGER.info("Triggering event: {}", stopStackRequest);
        eventBus.notify(stopStackRequest.selector(), Event.wrap(stopStackRequest));
        try {
            StopInstancesResult res = promise.await(1, TimeUnit.HOURS);
            LOGGER.info("Result: {}", res);
        } catch (InterruptedException e) {
            LOGGER.error("Error while stopping stack: ", e);
        }
    }

    @Override
    public Set<Resource> addInstances(Stack stack, String gateWayUserData, String coreUserData, Integer instanceCount, String instanceGroup) {
        //TODO
        return null;
    }

    @Override
    public Set<String> removeInstances(Stack stack, Set<String> instanceIds, String instanceGroup) {
        //TODO
        return null;
    }

    @Override
    public void deleteStack(Stack stack, Credential credential) {
        LOGGER.debug("Assembling terminate stack event for stack: {}", stack);
        CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), stack.cloudPlatform().name());
        CloudCredential cloudCredential = buildCloudCredential(stack);
        Promise<TerminateStackResult> promise = Promises.prepare();
        //TODO which resources?
        Set<Resource> jpaResult = stack.getResources();
        List<CloudResource> resources = new ArrayList<>();
        for (Resource resource : jpaResult) {
            resources.add(new CloudResource(resource.getResourceType(), stack.getName(), resource.getResourceName()));
        }
        TerminateStackRequest terminateStackRequest = new TerminateStackRequest(cloudContext, cloudCredential, resources, promise);
        LOGGER.info("Triggering terminate stack event: {}", terminateStackRequest);
        eventBus.notify(terminateStackRequest.selector(), Event.wrap(terminateStackRequest));
        try {
            TerminateStackResult res = promise.await(1, TimeUnit.HOURS);
            LOGGER.info("Terminate stack result: {}", res);
            if (res == null) {
                throw new OpenStackResourceException("Stack termination failed: the termination of resource timed out.");
                //TODO shouldn't we allow cluster delete then, what if someone deletes the stack by hand?
            } else if (res.getStatus().equals(EventStatus.FAILED)) {
                throw new OpenStackResourceException(res.getStatusReason(), res.getErrorDetails());
            }
        } catch (InterruptedException e) {
            LOGGER.error("Error while terminating stack: ", e);
        }
    }

    @Override
    public void rollback(Stack stack, Set<Resource> resourceSet) {
        //TODO
    }

    @Override
    public void updateAllowedSubnets(Stack stack, String gateWayUserData, String coreUserData) {
        //TODO
    }

    @Override
    public String getSSHUser(Map<String, String> context) {
        CloudContext cloudContext = new CloudContext(null, null, context.get(ProvisioningService.PLATFORM));
        Promise<SshUserResponse> promise = Promises.prepare();
        SshUserRequest<SshUserResponse> sshUserRequest = new SshUserRequest<>(cloudContext, promise);
        LOGGER.info("Triggering event: {}", sshUserRequest);
        eventBus.notify(CloudPlatformRequest.selector(SshUserRequest.class), Event.wrap(sshUserRequest));
        SshUserResponse response = null;
        try {
            response = promise.await(1, TimeUnit.HOURS);
            LOGGER.info("Result: {}", response);
        } catch (InterruptedException e) {
            LOGGER.error("Error while retrieving ssh user", e);
        }
        return response.getUser();
    }

    @Override
    public Set<CoreInstanceMetaData> collectMetadata(Stack stack) {
        CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), stack.cloudPlatform().name());
        CloudCredential cloudCredential = credentialConverter.convert(stack.getCredential());
        List<InstanceTemplate> instanceTemplates = cloudStackConverter.buildInstanceTemplates(stack);
        List<CloudResource> cloudResources = cloudResourceConverter.convert(stack.getResources());
        Promise<CollectMetadataResult> promise = Promises.prepare();
        CollectMetadataRequest cmr = new CollectMetadataRequest(cloudContext, cloudCredential, cloudResources, instanceTemplates, promise);
        LOGGER.info("Triggering event: {}", cmr);
        eventBus.notify(cmr.selector(CollectMetadataRequest.class), Event.wrap(cmr));
        CollectMetadataResult res;
        try {
            res = promise.await(1, TimeUnit.HOURS);
            LOGGER.info("Result: {}", res);
            return new HashSet<>(instanceConverter.convert(res.getResults()));
        } catch (InterruptedException e) {
            LOGGER.error("Error while executing pre-provision check", e);
            throw new RuntimeException("Failed to collect metadata");

        }
    }

    @Override
    public Set<CoreInstanceMetaData> collectNewMetadata(Stack stack, Set<Resource> resourceList, String instanceGroup) {
        //TODO
        return null;
    }

    @Override
    public InstanceSyncState getState(Stack stack, InstanceGroup instanceGroup, String instanceId) {
        //TODO
        return null;
    }

    @Override
    public ResourceType getInstanceResourceType() {
        //TODO
        return null;
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.ADAPTER;
    }


    @Override
    public Set<String> getSSHFingerprints(Stack stack, String gateway) {
        Set<String> result = new HashSet<>();
        Resource heatResource = stack.getResourceByType(ResourceType.HEAT_STACK);
        if (heatResource == null) {
            String errorMessage = String.format("No Heat resource is referenced with this stack: %s, unable to get ssh fingerprints", stack.getId());
            LOGGER.info(errorMessage);
            throw new OpenStackResourceException(errorMessage);
        }
        LOGGER.debug("Get SSH fingerprints of gateway instance for stack: {}", stack);
        CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), CloudPlatform.OPENSTACK.name());
        CloudCredential cloudCredential = buildCloudCredential(stack);
        Promise<GetSSHFingerprintsResult> promise = Promises.prepare();
        InstanceMetaData gatewayMetaData = stack.getGatewayInstanceGroup().getInstanceMetaData().iterator().next();
        CloudInstance gatewayInstance = metadataConverter.convert(gatewayMetaData);
        GetSSHFingerprintsRequest getSSHFingerprintsRequest = new GetSSHFingerprintsRequest(cloudContext, cloudCredential, promise, gatewayInstance);
        LOGGER.info("Triggering GetSSHFingerprintsRequest stack event: {}", getSSHFingerprintsRequest);
        eventBus.notify(getSSHFingerprintsRequest.selector(), Event.wrap(getSSHFingerprintsRequest));
        try {
            GetSSHFingerprintsResult res = promise.await(1, TimeUnit.HOURS);
            LOGGER.info("Get SSH fingerprints of gateway instance for stack result: {}", res);
            if (res == null) {
                throw new OpenStackResourceException("Failed to get SSH fingerprints of gateway instance: the termination of resource timed out.");
            } else if (res.getStatus().equals(EventStatus.FAILED)) {
                throw new OpenStackResourceException(res.getStatusReason(), res.getErrorDetails());
            }
            result.addAll(res.getSshFingerprints());
        } catch (InterruptedException e) {
            LOGGER.error("Failed to get SSH fingerprints of gateway instance: ", e);
        }
        return result;
    }

    private CloudCredential buildCloudCredential(Stack stack) {
        OpenStackCredential openstackCredential = (OpenStackCredential) stack.getCredential();
        return new CloudCredential(openstackCredential.getName(), getDeclaredFields(openstackCredential));
    }

    private Set<Resource> transformResults(List<CloudResourceStatus> cloudResourceStatuses, Stack stack) {
        Set<Resource> retSet = new HashSet<>();
        for (CloudResourceStatus cloudResourceStatus : cloudResourceStatuses) {
            Resource resource = new Resource(cloudResourceStatus.getCloudResource().getType(), cloudResourceStatus.getCloudResource().getReference(), stack);
            retSet.add(resource);
        }
        return retSet;
    }
}
