package com.sequenceiq.cloudbreak.service.stack.connector.openstack;

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
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.event.LaunchStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.LaunchStackResult;
import com.sequenceiq.cloudbreak.cloud.event.TerminateStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.TerminateStackResult;
import com.sequenceiq.cloudbreak.cloud.event.context.StackContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.Instance;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.cloud.model.Subnet;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceGroupType;
import com.sequenceiq.cloudbreak.domain.OpenStackCredential;
import com.sequenceiq.cloudbreak.domain.OpenStackNetwork;
import com.sequenceiq.cloudbreak.domain.OpenStackTemplate;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
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
    private PBEStringCleanablePasswordEncryptor pbeStringCleanablePasswordEncryptor;


    @Override
    public Set<Resource> buildStack(Stack stack, String gateWayUserData, String coreUserData, Map<String, Object> setupProperties) {
        LOGGER.info("Assembling launch request for stack: {}", stack);
        LaunchStackResult res = null;
        StackContext stackContext = new StackContext(stack.getId(), stack.getName(), CloudPlatform.OPENSTACK.name());
        CloudCredential cloudCredential = buildCloudCredential(stack);

        List<Group> groups = getGroups(stack);

        Image image = new Image(stack.getImage());
        image.putUserData(InstanceGroupType.CORE, coreUserData);
        image.putUserData(InstanceGroupType.GATEWAY, gateWayUserData);

        OpenStackNetwork openStackNetwork = (OpenStackNetwork) stack.getNetwork();
        Subnet subnet = new Subnet(openStackNetwork.getSubnetCIDR());
        Network network = new Network(subnet);
        network.putParameter("publicNetId", openStackNetwork.getPublicNetId());

        Security security = new Security();
        for (com.sequenceiq.cloudbreak.domain.Subnet cbSubNet : stack.getAllowedSubnets()) {
            Subnet sn = new Subnet(cbSubNet.getCidr());
            security.addAllowedSubnet(sn);
        }

        CloudStack cloudStack = new CloudStack(groups, network, security, image);
        Promise<LaunchStackResult> promise = Promises.prepare();
        LaunchStackRequest launchStackRequest = new LaunchStackRequest(stackContext, cloudCredential, cloudStack, promise);

        LOGGER.info("Triggering event: {}", launchStackRequest);
        eventBus.notify(launchStackRequest.selector(LaunchStackRequest.class), Event.wrap(launchStackRequest));
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
        LOGGER.debug("Assembling terminate stack event for stack: {}", stack);
        StackContext stackContext = new StackContext(stack.getId(), stack.getName(), CloudPlatform.OPENSTACK.name());
        CloudCredential cloudCredential = buildCloudCredential(stack);
        Promise<TerminateStackResult> promise = Promises.prepare();
        TerminateStackRequest terminateStackRequest = new TerminateStackRequest(stackContext, cloudCredential, promise);
        String reference = stack.getResourceByType(ResourceType.HEAT_STACK) != null
                ? stack.getResourceByType(ResourceType.HEAT_STACK).getResourceName() : null;

        CloudResource cloudResource = new CloudResource(ResourceType.HEAT_STACK, stack.getName(), reference);
        terminateStackRequest.getCloudResources().add(cloudResource);

        LOGGER.info("Triggering terminate stack event: {}", terminateStackRequest);
        eventBus.notify(terminateStackRequest.selector(TerminateStackRequest.class), Event.wrap(terminateStackRequest));
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
    public boolean startAll(Stack stack) {
        return false;
    }

    @Override
    public boolean stopAll(Stack stack) {
        return false;
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return null;
    }

    @Override
    public void updateAllowedSubnets(Stack stack, String gateWayUserData, String coreUserData) {

    }

    public List<Group> getGroups(Stack stack) {
        List<Group> groups = new ArrayList<>();
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {

            Group group = new Group(instanceGroup.getGroupName(), instanceGroup.getInstanceGroupType());

            for (int nodeId = 0; nodeId < instanceGroup.getNodeCount(); nodeId++) {
                OpenStackTemplate openStackTemplate = (OpenStackTemplate) instanceGroup.getTemplate();
                Instance instance = new Instance(openStackTemplate.getInstanceType(), group.getName(), nodeId);

                for (int i = 0; i < openStackTemplate.getVolumeCount(); i++) {
                    Volume volume = new Volume("/hadoop/fs" + i, "HDD", openStackTemplate.getVolumeSize());
                    instance.addVolume(volume);
                }
                group.addInstance(instance);
            }
            groups.add(group);
        }
        return groups;
    }

    private CloudCredential buildCloudCredential(Stack stack) {
        OpenStackCredential openstackCredential = (OpenStackCredential) stack.getCredential();
        CloudCredential cloudCredential = new CloudCredential(openstackCredential.getName());
        cloudCredential.putParameter("userName", pbeStringCleanablePasswordEncryptor.decrypt(openstackCredential.getUserName()));
        cloudCredential.putParameter("password", pbeStringCleanablePasswordEncryptor.decrypt(openstackCredential.getPassword()));
        cloudCredential.putParameter("tenantName", openstackCredential.getTenantName());
        cloudCredential.putParameter("endpoint", openstackCredential.getEndpoint());
        return cloudCredential;
    }
}
