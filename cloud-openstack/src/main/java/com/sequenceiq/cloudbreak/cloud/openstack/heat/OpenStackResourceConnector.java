package com.sequenceiq.cloudbreak.cloud.openstack.heat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.heat.Stack;
import org.openstack4j.model.heat.StackUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.api.model.AdjustmentType;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.openstack.auth.OpenStackClient;
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackUtils;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

@Service
public class OpenStackResourceConnector implements ResourceConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackResourceConnector.class);
    private static final long OPERATION_TIMEOUT = 60L;

    @Inject
    private OpenStackClient openStackClient;
    @Inject
    private HeatTemplateBuilder heatTemplateBuilder;
    @Inject
    private OpenStackUtils utils;

    @SuppressWarnings("unchecked")
    @Override
    public List<CloudResourceStatus> launch(AuthenticatedContext authenticatedContext, CloudStack stack, PersistenceNotifier notifier,
            AdjustmentType adjustmentType, Long threshold) {
        String stackName = utils.getStackName(authenticatedContext);
        boolean existingNetwork = isExistingNetwork(stack);
        boolean assignFloatingIp = assignFloatingIp(stack);
        String existingSubnetCidr = getExistingSubnetCidr(authenticatedContext, stack);
        String heatTemplate = heatTemplateBuilder.build(
                stackName, stack.getGroups(), stack.getImage(), existingNetwork, existingSubnetCidr != null, assignFloatingIp);
        Map<String, String> parameters = heatTemplateBuilder.buildParameters(
                authenticatedContext, stack.getNetwork(), stack.getImage(), existingNetwork, existingSubnetCidr);

        OSClient client = openStackClient.createOSClient(authenticatedContext);

        Stack heatStack = client
                .heat()
                .stacks()
                .create(Builders.stack().name(stackName).template(heatTemplate).disableRollback(false)
                        .parameters(parameters).timeoutMins(OPERATION_TIMEOUT).build());


        CloudResource cloudResource = new CloudResource.Builder().type(ResourceType.HEAT_STACK).name(heatStack.getId()).build();
        try {
            notifier.notifyAllocation(cloudResource, authenticatedContext.getCloudContext());
        } catch (Exception e) {
            //Rollback
            terminate(authenticatedContext, stack, Lists.newArrayList(cloudResource));
        }
        List<CloudResourceStatus> resources = check(authenticatedContext, Lists.newArrayList(cloudResource));
        LOGGER.debug("Launched resources: {}", resources);
        return resources;
    }

    @Override
    public List<CloudResourceStatus> check(AuthenticatedContext authenticatedContext, List<CloudResource> resources) {
        List<CloudResourceStatus> result = new ArrayList<>();
        OSClient client = openStackClient.createOSClient(authenticatedContext);

        for (CloudResource resource : resources) {
            switch (resource.getType()) {
                case HEAT_STACK:
                    String heatStackId = resource.getName();
                    String stackName = utils.getStackName(authenticatedContext);
                    LOGGER.info("Checking OpenStack Heat stack status of: {}", stackName);
                    Stack heatStack = client.heat().stacks().getDetails(stackName, heatStackId);
                    CloudResourceStatus heatResourceStatus = utils.heatStatus(resource, heatStack);
                    result.add(heatResourceStatus);
                    break;
                default:
                    throw new CloudConnectorException(String.format("Invalid resource type: %s", resource.getType()));
            }
        }

        return result;
    }

    @Override
    public List<CloudResourceStatus> terminate(AuthenticatedContext authenticatedContext, CloudStack cloudStack, List<CloudResource> resources) {

        for (CloudResource resource : resources) {
            switch (resource.getType()) {
                case HEAT_STACK:
                    String heatStackId = resource.getName();
                    String stackName = utils.getStackName(authenticatedContext);
                    LOGGER.info("Terminate stack: {}", stackName);
                    OSClient client = openStackClient.createOSClient(authenticatedContext);
                    client.heat().stacks().delete(stackName, heatStackId);
                    break;
                default:
                    throw new CloudConnectorException(String.format("Invalid resource type: %s", resource.getType()));
            }
        }

        return check(authenticatedContext, resources);
    }

    @Override
    public List<CloudResourceStatus> upscale(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources) {
        String stackName = utils.getStackName(authenticatedContext);
        boolean existingNetwork = isExistingNetwork(stack);
        boolean assignFloatingIp = assignFloatingIp(stack);
        String existingSubnetCidr = getExistingSubnetCidr(authenticatedContext, stack);
        String heatTemplate = heatTemplateBuilder.build(
                stackName, stack.getGroups(), stack.getImage(), existingNetwork, existingSubnetCidr != null, assignFloatingIp);
        Map<String, String> parameters = heatTemplateBuilder.buildParameters(
                authenticatedContext, stack.getNetwork(), stack.getImage(), existingNetwork, existingSubnetCidr);
        return updateHeatStack(authenticatedContext, resources, heatTemplate, parameters);
    }

    @Override
    public List<CloudResourceStatus> downscale(AuthenticatedContext auth, CloudStack cloudStack, List<CloudResource> resources, List<CloudInstance> vms) {
        CloudStack stack = removeDeleteRequestedInstances(cloudStack);
        String stackName = utils.getStackName(auth);
        boolean existingNetwork = isExistingNetwork(stack);
        boolean assignFloatingIp = assignFloatingIp(stack);
        String existingSubnetCidr = getExistingSubnetCidr(auth, stack);
        String heatTemplate = heatTemplateBuilder.build(
                stackName, stack.getGroups(), stack.getImage(), existingNetwork, existingSubnetCidr != null, assignFloatingIp);
        Map<String, String> parameters = heatTemplateBuilder.buildParameters(
                auth, stack.getNetwork(), stack.getImage(), existingNetwork, existingSubnetCidr);
        return updateHeatStack(auth, resources, heatTemplate, parameters);
    }

    @Override
    public List<CloudResourceStatus> update(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources) {
        String stackName = utils.getStackName(authenticatedContext);
        boolean existingNetwork = isExistingNetwork(stack);
        boolean assignFloatingIp = assignFloatingIp(stack);
        String existingSubnetCidr = getExistingSubnetCidr(authenticatedContext, stack);
        String heatTemplate = heatTemplateBuilder.build(
                stackName, stack.getGroups(), stack.getImage(), existingNetwork, existingSubnetCidr != null, assignFloatingIp);
        Map<String, String> parameters = heatTemplateBuilder.buildParameters(
                authenticatedContext, stack.getNetwork(), stack.getImage(), existingNetwork, existingSubnetCidr);
        return updateHeatStack(authenticatedContext, resources, heatTemplate, parameters);
    }

    private List<CloudResourceStatus> updateHeatStack(AuthenticatedContext authenticatedContext, List<CloudResource> resources, String heatTemplate,
            Map<String, String> parameters) {
        CloudResource resource = utils.getHeatResource(resources);
        String stackName = utils.getStackName(authenticatedContext);
        String heatStackId = resource.getName();

        OSClient client = openStackClient.createOSClient(authenticatedContext);
        StackUpdate updateRequest = Builders.stackUpdate().template(heatTemplate)
                .parameters(parameters).timeoutMins(OPERATION_TIMEOUT).build();
        client.heat().stacks().update(stackName, heatStackId, updateRequest);
        LOGGER.info("Heat stack update request sent with stack name: '{}' for Heat stack: '{}'", stackName, heatStackId);
        return check(authenticatedContext, resources);
    }

    private CloudStack removeDeleteRequestedInstances(CloudStack stack) {
        List<Group> groups = new ArrayList<>();
        for (Group group : stack.getGroups()) {
            List<CloudInstance> instances = new ArrayList<>(group.getInstances());
            for (CloudInstance instance : group.getInstances()) {
                if (InstanceStatus.DELETE_REQUESTED == instance.getTemplate().getStatus()) {
                    instances.remove(instance);
                }
            }
            groups.add(new Group(group.getName(), group.getType(), instances, group.getSecurity()));
        }
        return new CloudStack(groups, stack.getNetwork(), stack.getImage(), stack.getParameters());
    }

    private boolean isExistingNetwork(CloudStack stack) {
        return utils.isExistingNetwork(stack.getNetwork());
    }

    private boolean assignFloatingIp(CloudStack stack) {
        return utils.assignFloatingIp(stack.getNetwork());
    }

    private String getExistingSubnetCidr(AuthenticatedContext authenticatedContext, CloudStack stack) {
        Network network = stack.getNetwork();
        return utils.isExistingSubnet(network) ? utils.getExistingSubnetCidr(authenticatedContext, network) : null;
    }

}
