package com.sequenceiq.cloudbreak.cloud.openstack.heat;

import static com.google.common.collect.Lists.newArrayList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Keypair;
import org.openstack4j.model.heat.Stack;
import org.openstack4j.model.heat.StackUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.AdjustmentType;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource.Builder;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.TlsInfo;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.openstack.auth.OpenStackClient;
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants;
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackUtils;
import com.sequenceiq.cloudbreak.cloud.openstack.heat.HeatTemplateBuilder.ModelContext;
import com.sequenceiq.cloudbreak.cloud.openstack.view.KeystoneCredentialView;
import com.sequenceiq.cloudbreak.cloud.openstack.view.NeutronNetworkView;
import com.sequenceiq.cloudbreak.common.type.ResourceType;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.cloudbreak.service.Retry.ActionWentFailException;

@Service
public class OpenStackResourceConnector implements ResourceConnector<Object> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackResourceConnector.class);

    private static final long OPERATION_TIMEOUT = 60L;

    @Inject
    private OpenStackClient openStackClient;

    @Inject
    private HeatTemplateBuilder heatTemplateBuilder;

    @Inject
    private OpenStackUtils utils;

    @Inject
    @Qualifier("DefaultRetryService")
    private Retry retryService;

    @Inject
    private PersistenceNotifier persistenceNotifier;

    @Override
    public List<CloudResourceStatus> launch(AuthenticatedContext authenticatedContext, CloudStack stack, PersistenceNotifier notifier,
            AdjustmentType adjustmentType, Long threshold) {
        String stackName = utils.getStackName(authenticatedContext);
        NeutronNetworkView neutronNetworkView = new NeutronNetworkView(stack.getNetwork());
        boolean existingNetwork = neutronNetworkView.isExistingNetwork();
        String existingSubnetCidr = getExistingSubnetCidr(authenticatedContext, stack);

        ModelContext modelContext = new ModelContext();
        modelContext.withExistingNetwork(existingNetwork);
        modelContext.withExistingSubnet(existingSubnetCidr != null);
        modelContext.withGroups(stack.getGroups());
        modelContext.withInstanceUserData(stack.getImage());
        modelContext.withLocation(authenticatedContext.getCloudContext().getLocation());
        modelContext.withStackName(stackName);
        modelContext.withNeutronNetworkView(neutronNetworkView);
        modelContext.withTemplateString(stack.getTemplate());
        modelContext.withTags(stack.getTags());

        String heatTemplate = heatTemplateBuilder.build(modelContext);
        Map<String, String> parameters = heatTemplateBuilder.buildParameters(
                authenticatedContext, stack, existingNetwork, existingSubnetCidr);

        OSClient<?> client = openStackClient.createOSClient(authenticatedContext);

        List<CloudResourceStatus> resources;
        Stack existingStack = client.heat().stacks().getStackByName(stackName);

        if (existingStack == null) {
            if (stack.getInstanceAuthentication().getPublicKeyId() == null) {
                createKeyPair(authenticatedContext, stack, client);
            }
            Stack heatStack = client
                    .heat()
                    .stacks()
                    .create(Builders.stack().name(stackName).template(heatTemplate).disableRollback(false)
                            .parameters(parameters).timeoutMins(OPERATION_TIMEOUT).build());
            List<CloudResource> cloudResources = collectResources(authenticatedContext, notifier, heatStack, stack, neutronNetworkView);
            resources = check(authenticatedContext, cloudResources);
        } else {
            LOGGER.debug("Heat stack already exists: {}", existingStack.getName());
            List<CloudResource> cloudResources = collectResources(authenticatedContext, notifier, existingStack, stack, neutronNetworkView);
            resources = check(authenticatedContext, cloudResources);
        }
        LOGGER.debug("Launched resources: {}", resources);
        return resources;
    }

    private List<CloudResource> collectResources(AuthenticatedContext authenticatedContext, PersistenceNotifier notifier, Stack heatStack, CloudStack stack,
            NeutronNetworkView neutronNetworkView) {
        List<CloudResource> cloudResources = newArrayList();

        CloudResource heatResource = new Builder().type(ResourceType.HEAT_STACK).name(heatStack.getId()).build();
        try {
            notifier.notifyAllocation(heatResource, authenticatedContext.getCloudContext());
        } catch (RuntimeException e) {
            //Rollback
            LOGGER.warn("Error occured: {}, OpenstackResourceConnector is rolling back", e.getMessage());
            terminate(authenticatedContext, stack, Collections.singletonList(heatResource));
        }
        cloudResources.add(heatResource);
        if (!neutronNetworkView.isProviderNetwork()) {
            if (!neutronNetworkView.isExistingNetwork()) {
                CloudResource r = CloudResource.builder().type(ResourceType.OPENSTACK_NETWORK).name(OpenStackConstants.NETWORK_ID).build();
                cloudResources.add(r);
            }
            if (!neutronNetworkView.isExistingSubnet()) {
                CloudResource r = CloudResource.builder().type(ResourceType.OPENSTACK_SUBNET).name(OpenStackConstants.SUBNET_ID).build();
                cloudResources.add(r);
            }
        }
        return cloudResources;
    }

    private void createKeyPair(AuthenticatedContext authenticatedContext, CloudStack stack, OSClient<?> client) {
        KeystoneCredentialView keystoneCredential = openStackClient.createKeystoneCredential(authenticatedContext);

        String keyPairName = keystoneCredential.getKeyPairName();
        if (client.compute().keypairs().get(keyPairName) == null) {
            try {
                Keypair keyPair = client.compute().keypairs().create(keyPairName, stack.getInstanceAuthentication().getPublicKey());
                LOGGER.debug("Keypair has been created: {}", keyPair);
            } catch (Exception e) {
                LOGGER.warn("Failed to create keypair", e);
                throw new CloudConnectorException(e.getMessage(), e);
            }
        } else {
            LOGGER.debug("Keypair already exists: {}", keyPairName);
        }
    }

    @Override
    public List<CloudResourceStatus> check(AuthenticatedContext authenticatedContext, List<CloudResource> resources) {
        List<CloudResourceStatus> results = newArrayList();
        OSClient<?> client = openStackClient.createOSClient(authenticatedContext);
        String stackName = utils.getStackName(authenticatedContext);
        List<CloudResource> osResourceList = openStackClient.getResources(stackName, authenticatedContext.getCloudCredential());
        List<CloudResource> otherResources = newArrayList();
        CloudResourceStatus heatStatus = null;
        for (CloudResource resource : resources) {
            if (resource.getType() == ResourceType.HEAT_STACK) {
                heatStatus = checkByResourceType(authenticatedContext, client, stackName, osResourceList, resource);
                results.add(heatStatus);
            } else {
                otherResources.add(resource);
            }
        }
        if (heatStatus != null) {
            for (CloudResource resource : otherResources) {
                if (heatStatus.getStatus() == ResourceStatus.CREATED) {
                    results.add(checkByResourceType(authenticatedContext, client, stackName, osResourceList, resource));
                } else {
                    results.add(new CloudResourceStatus(resource, ResourceStatus.CREATED));
                }
            }
        }
        return results;
    }

    private CloudResourceStatus checkByResourceType(AuthenticatedContext authenticatedContext, OSClient<?> client,
            String stackName, Collection<CloudResource> list, CloudResource resource) {
        CloudResourceStatus result = null;
        switch (resource.getType()) {
            case HEAT_STACK:
                String heatStackId = resource.getName();
                LOGGER.debug("Checking OpenStack Heat stack status of: {}", stackName);
                Stack heatStack = Optional.ofNullable(client.heat().stacks().getDetails(stackName, heatStackId))
                        .orElseThrow(() -> new CloudConnectorException("Stack not found on provider by id: " + heatStackId));
                result = utils.heatStatus(resource, heatStack);
                break;
            case OPENSTACK_NETWORK:
                result = checkResourceStatus(authenticatedContext, stackName, list, resource, ResourceType.OPENSTACK_NETWORK);
                break;
            case OPENSTACK_SUBNET:
                result = checkResourceStatus(authenticatedContext, stackName, list, resource, ResourceType.OPENSTACK_SUBNET);
                break;
            case OPENSTACK_ROUTER:
            case OPENSTACK_INSTANCE:
            case OPENSTACK_PORT:
            case OPENSTACK_ATTACHED_DISK:
            case OPENSTACK_SECURITY_GROUP:
            case OPENSTACK_FLOATING_IP:
                break;
            default:
                throw new CloudConnectorException(String.format("Invalid resource type: %s", resource.getType()));
        }
        return result;
    }

    private CloudResourceStatus checkResourceStatus(AuthenticatedContext authenticatedContext, String stackName, Collection<CloudResource> list,
            CloudResource resource, ResourceType openstackNetwork) {
        CloudResourceStatus result;
        result = getCloudResourceStatus(list, openstackNetwork, resource);
        if (result.getStatus().isPermanent()) {
            persistenceNotifier.notifyAllocation(result.getCloudResource(), authenticatedContext.getCloudContext());
        }
        return result;
    }

    private CloudResourceStatus getCloudResourceStatus(Collection<CloudResource> resources, ResourceType resourceType, CloudResource resource) {
        return resources.stream()
                .filter(r -> r.getType() == resourceType)
                .findFirst()
                .map(cloudResource -> new CloudResourceStatus(cloudResource, ResourceStatus.UPDATED))
                .orElseGet(() -> new CloudResourceStatus(resource, ResourceStatus.IN_PROGRESS));
    }

    @Override
    public List<CloudResourceStatus> terminate(AuthenticatedContext authenticatedContext, CloudStack cloudStack, List<CloudResource> resources) {
        List<CloudResource> resourceForTermination = resources.stream()
                .filter(r -> r.getType() == ResourceType.HEAT_STACK)
                .collect(Collectors.toList());

        resourceForTermination.forEach(r -> terminateHeatStack(authenticatedContext, cloudStack, r));
        return check(authenticatedContext, resourceForTermination);
    }

    private void terminateHeatStack(AuthenticatedContext authenticatedContext, CloudStack cloudStack, CloudResource resource) {
        String heatStackId = resource.getName();
        String stackName = utils.getStackName(authenticatedContext);
        LOGGER.debug("Terminate stack: {}", stackName);
        OSClient<?> client = openStackClient.createOSClient(authenticatedContext);
        try {
            retryService.testWith2SecDelayMax5Times(() -> {
                boolean exists = client.heat().stacks().getStackByName(resource.getName()) != null;
                if (!exists) {
                    throw new ActionWentFailException("Stack not exists");
                }
                return true;
            });
            client.heat().stacks().delete(stackName, heatStackId);
            LOGGER.debug("Heat stack has been deleted");
            if (cloudStack.getInstanceAuthentication().getPublicKeyId() == null) {
                deleteKeyPair(authenticatedContext, client);
            }
        } catch (ActionWentFailException ignored) {
            LOGGER.debug("Stack not found with name: {}", resource.getName());
        }
    }

    private void deleteKeyPair(AuthenticatedContext authenticatedContext, OSClient<?> client) {
        KeystoneCredentialView keystoneCredential = openStackClient.createKeystoneCredential(authenticatedContext);
        String keyPairName = keystoneCredential.getKeyPairName();
        client.compute().keypairs().delete(keyPairName);
        LOGGER.debug("Keypair has been deleted: {}", keyPairName);
    }

    @Override
    public List<CloudResourceStatus> upscale(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources) {
        String stackName = utils.getStackName(authenticatedContext);
        NeutronNetworkView neutronNetworkView = new NeutronNetworkView(stack.getNetwork());
        boolean existingNetwork = neutronNetworkView.isExistingNetwork();
        String existingSubnetCidr = getExistingSubnetCidr(authenticatedContext, stack);

        ModelContext modelContext = new ModelContext();
        modelContext.withExistingNetwork(existingNetwork);
        modelContext.withExistingSubnet(existingSubnetCidr != null);
        modelContext.withGroups(stack.getGroups());
        modelContext.withInstanceUserData(stack.getImage());
        modelContext.withLocation(authenticatedContext.getCloudContext().getLocation());
        modelContext.withStackName(stackName);
        modelContext.withNeutronNetworkView(neutronNetworkView);
        modelContext.withTemplateString(stack.getTemplate());
        modelContext.withTags(stack.getTags());

        String heatTemplate = heatTemplateBuilder.build(modelContext);
        Map<String, String> parameters = heatTemplateBuilder.buildParameters(
                authenticatedContext, stack, existingNetwork, existingSubnetCidr);
        return updateHeatStack(authenticatedContext, resources, heatTemplate, parameters);
    }

    @Override
    public Object collectResourcesToRemove(AuthenticatedContext authenticatedContext, CloudStack stack,
            List<CloudResource> resources, List<CloudInstance> vms) {
        return null;
    }

    @Override
    public List<CloudResourceStatus> downscale(AuthenticatedContext authenticatedContext, CloudStack cloudStack, List<CloudResource> resources,
            List<CloudInstance> vms, Object resourcesToRemove) {
        CloudStack stack = removeDeleteRequestedInstances(cloudStack);
        String stackName = utils.getStackName(authenticatedContext);
        NeutronNetworkView neutronNetworkView = new NeutronNetworkView(stack.getNetwork());
        boolean existingNetwork = neutronNetworkView.isExistingNetwork();
        String existingSubnetCidr = getExistingSubnetCidr(authenticatedContext, stack);

        ModelContext modelContext = new ModelContext();
        modelContext.withExistingNetwork(existingNetwork);
        modelContext.withExistingSubnet(existingSubnetCidr != null);
        modelContext.withGroups(stack.getGroups());
        modelContext.withInstanceUserData(stack.getImage());
        modelContext.withLocation(authenticatedContext.getCloudContext().getLocation());
        modelContext.withStackName(stackName);
        modelContext.withNeutronNetworkView(neutronNetworkView);
        modelContext.withTemplateString(stack.getTemplate());
        modelContext.withTags(stack.getTags());

        String heatTemplate = heatTemplateBuilder.build(modelContext);
        Map<String, String> parameters = heatTemplateBuilder.buildParameters(
                authenticatedContext, stack, existingNetwork, existingSubnetCidr);
        return updateHeatStack(authenticatedContext, resources, heatTemplate, parameters);
    }

    @Override
    public TlsInfo getTlsInfo(AuthenticatedContext authenticatedContext, CloudStack cloudStack) {
        return new TlsInfo(false);
    }

    @Override
    public String getStackTemplate() {
        return heatTemplateBuilder.getTemplate();
    }

    @Override
    public List<CloudResourceStatus> update(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources) {
        String stackName = utils.getStackName(authenticatedContext);
        NeutronNetworkView neutronNetworkView = new NeutronNetworkView(stack.getNetwork());
        boolean existingNetwork = neutronNetworkView.isExistingNetwork();
        String existingSubnetCidr = getExistingSubnetCidr(authenticatedContext, stack);

        ModelContext modelContext = new ModelContext();
        modelContext.withExistingNetwork(existingNetwork);
        modelContext.withExistingSubnet(existingSubnetCidr != null);
        modelContext.withGroups(stack.getGroups());
        modelContext.withInstanceUserData(stack.getImage());
        modelContext.withLocation(authenticatedContext.getCloudContext().getLocation());
        modelContext.withStackName(stackName);
        modelContext.withNeutronNetworkView(neutronNetworkView);
        modelContext.withTemplateString(stack.getTemplate());
        modelContext.withTags(stack.getTags());

        String heatTemplate = heatTemplateBuilder.build(modelContext);
        Map<String, String> parameters = heatTemplateBuilder.buildParameters(
                authenticatedContext, stack, existingNetwork, existingSubnetCidr);
        return updateHeatStack(authenticatedContext, resources, heatTemplate, parameters);
    }

    private List<CloudResourceStatus> updateHeatStack(AuthenticatedContext authenticatedContext, List<CloudResource> resources, String heatTemplate,
            Map<String, String> parameters) {
        CloudResource resource = utils.getHeatResource(resources);
        String stackName = utils.getStackName(authenticatedContext);
        String heatStackId = resource.getName();

        OSClient<?> client = openStackClient.createOSClient(authenticatedContext);
        StackUpdate updateRequest = Builders.stackUpdate().template(heatTemplate)
                .parameters(parameters).timeoutMins(OPERATION_TIMEOUT).build();
        client.heat().stacks().update(stackName, heatStackId, updateRequest);
        LOGGER.debug("Heat stack update request sent with stack name: '{}' for Heat stack: '{}'", stackName, heatStackId);
        return check(authenticatedContext, resources);
    }

    private CloudStack removeDeleteRequestedInstances(CloudStack stack) {
        List<Group> groups = new ArrayList<>(stack.getGroups().size());
        for (Group group : stack.getGroups()) {
            List<CloudInstance> instances = new ArrayList<>(group.getInstances());
            for (CloudInstance instance : group.getInstances()) {
                if (InstanceStatus.DELETE_REQUESTED == instance.getTemplate().getStatus()) {
                    instances.remove(instance);
                }
            }
            groups.add(new Group(group.getName(), group.getType(), instances, group.getSecurity(), null, stack.getInstanceAuthentication(),
                    stack.getInstanceAuthentication().getLoginUserName(), stack.getInstanceAuthentication().getPublicKey(), group.getRootVolumeSize()));
        }
        return new CloudStack(groups, stack.getNetwork(), stack.getImage(), stack.getParameters(), stack.getTags(),
                stack.getTemplate(), stack.getInstanceAuthentication(), stack.getInstanceAuthentication().getLoginUserName(),
                stack.getInstanceAuthentication().getPublicKey(), stack.getFileSystem().orElse(null));
    }

    private String getExistingSubnetCidr(AuthenticatedContext authenticatedContext, CloudStack stack) {
        NeutronNetworkView neutronView = new NeutronNetworkView(stack.getNetwork());
        return neutronView.isExistingSubnet() ? utils.getExistingSubnetCidr(authenticatedContext, neutronView) : null;
    }

}
