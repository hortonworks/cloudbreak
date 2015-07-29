package com.sequenceiq.cloudbreak.service.stack.connector.openstack;

import static com.sequenceiq.cloudbreak.service.stack.flow.ReflectionUtils.getDeclaredFields;
import static java.util.Arrays.asList;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.jasypt.encryption.pbe.PBEStringCleanablePasswordEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.StartInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopInstancesResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.OpenStackCredential;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.SecurityRuleRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;

import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.rx.Promise;
import reactor.rx.Promises;

@Service
public class OpenStackConnectorV2Facade implements CloudPlatformConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackConnectorV2Facade.class);

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
    private InstanceMetaDataToCloudInstanceConverter instanceConverter;


    @Override
    public Set<Resource> buildStack(Stack stack, String gateWayUserData, String coreUserData, Map<String, Object> setupProperties) {
        LOGGER.info("Assembling launch request for stack: {}", stack);
        LaunchStackResult res = null;

        CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), CloudPlatform.OPENSTACK.name());
        CloudCredential cloudCredential = buildCloudCredential(stack);

        CloudStack cloudStack = cloudStackConverter.convert(stack, coreUserData, gateWayUserData);

        Promise<LaunchStackResult> promise = Promises.prepare();
        LaunchStackRequest launchStackRequest = new LaunchStackRequest(cloudContext, cloudCredential, cloudStack, promise);

        LOGGER.info("Triggering event: {}", launchStackRequest);
        eventBus.notify(launchStackRequest.selector(), Event.wrap(launchStackRequest));
        try {
            res = promise.await(1, TimeUnit.HOURS);
            LOGGER.info("Result: {}", res);
        } catch (InterruptedException e) {
            LOGGER.error("Error while launching stack: ", e);
        }

        return tranformResults(res.getResults(), stack);
    }

    private Set<Resource> tranformResults(List<CloudResourceStatus> cloudResourceStatuses, Stack stack) {
        Set<Resource> retSet = new HashSet<>();
        for (CloudResourceStatus cloudResourceStatus : cloudResourceStatuses) {
            Resource resource = new Resource(cloudResourceStatus.getCloudResource().getType(), cloudResourceStatus.getCloudResource().getReference(), stack);
            retSet.add(resource);
        }
        return retSet;
    }


    @Override
    public Set<Resource> addInstances(Stack stack, String gateWayUserData, String coreUserData, Integer instanceCount, String instanceGroup) {
        return null;
    }

    @Override
    public Set<String> removeInstances(Stack stack, Set<String> instanceIds, String instanceGroup) {
        return null;
    }

    @Override
    public void deleteStack(Stack stack, Credential credential) {
        Resource heatResource = stack.getResourceByType(ResourceType.HEAT_STACK);
        if (heatResource == null) {
            LOGGER.info("No Heat resource is referenced with this stack: {}, nothing to delete", stack.getId());
            return;
        }
        LOGGER.debug("Assembling terminate stack event for stack: {}", stack);
        CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), CloudPlatform.OPENSTACK.name());
        CloudCredential cloudCredential = buildCloudCredential(stack);
        Promise<TerminateStackResult> promise = Promises.prepare();
        String heatReference = heatResource.getResourceName();
        CloudResource cloudResource = new CloudResource(ResourceType.HEAT_STACK, stack.getName(), heatReference);
        TerminateStackRequest terminateStackRequest = new TerminateStackRequest(cloudContext, cloudCredential, asList(cloudResource), promise);
        LOGGER.info("Triggering terminate stack event: {}", terminateStackRequest);
        eventBus.notify(terminateStackRequest.selector(), Event.wrap(terminateStackRequest));
        try {
            TerminateStackResult res = promise.await();
            LOGGER.info("Terminate stack result: {}", res);
        } catch (InterruptedException e) {
            LOGGER.error("Error while terminating stack: ", e);
        }
    }

    @Override
    public void rollback(Stack stack, Set<Resource> resourceSet) {

    }

    @Override
    public void startAll(Stack stack) {
        Resource heatResource = stack.getResourceByType(ResourceType.HEAT_STACK);
        if (heatResource == null) {
            LOGGER.info("No Heat resource is referenced with this stack: {}, nothing to start", stack.getId());
            return;
        }
        LOGGER.info("Assembling start request for stack: {}", stack);
        CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), CloudPlatform.OPENSTACK.name());
        CloudCredential cloudCredential = buildCloudCredential(stack);
        List<CloudInstance> instances = instanceConverter.convert(stack.getInstanceMetaDataAsList());
        Promise<StartInstancesRequest> promise = Promises.prepare();
        StartInstancesRequest startStackRequest = new StartInstancesRequest(cloudContext, cloudCredential, instances, promise);
        LOGGER.info("Triggering event: {}", startStackRequest);
        eventBus.notify(startStackRequest.selector(), Event.wrap(startStackRequest));
        try {
            StartInstancesRequest res = promise.await(1, TimeUnit.HOURS);
            LOGGER.info("Result: {}", res);
        } catch (InterruptedException e) {
            LOGGER.error("Error while starting stack: ", e);
        }
    }

    @Override
    public void stopAll(Stack stack) {
        Resource heatResource = stack.getResourceByType(ResourceType.HEAT_STACK);
        if (heatResource == null) {
            LOGGER.info("No Heat resource is referenced with this stack: {}, nothing to stop", stack.getId());
            return;
        }
        LOGGER.info("Assembling stop request for stack: {}", stack);
        CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), CloudPlatform.OPENSTACK.name());
        CloudCredential cloudCredential = buildCloudCredential(stack);
        List<CloudInstance> instances = instanceConverter.convert(stack.getInstanceMetaDataAsList());
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
    public CloudPlatform getCloudPlatform() {
        return null;
    }

    @Override
    public void updateAllowedSubnets(Stack stack, String gateWayUserData, String coreUserData) {

    }

    @Override
    public String getSSHUser() {
        return null;
    }

    @Override
    public Set<String> getSSHFingerprints(Stack stack, String gateway) {
        return null;
    }

    private CloudCredential buildCloudCredential(Stack stack) {
        OpenStackCredential openstackCredential = (OpenStackCredential) stack.getCredential();
        return new CloudCredential(openstackCredential.getName(), getDeclaredFields(openstackCredential));
    }
}
