package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.RESOURCE_GROUP_NAME_PARAMETER;
import static com.sequenceiq.cloudbreak.cloud.model.CloudResource.PRIVATE_ID;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.compute.OSDisk;
import com.microsoft.azure.management.compute.StorageProfile;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.network.Subnet;
import com.microsoft.azure.management.resources.Deployment;
import com.microsoft.azure.management.resources.DeploymentOperation;
import com.microsoft.azure.management.resources.TargetResource;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@Component
public class AzureCloudResourceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureCloudResourceService.class);

    @Inject
    private AzureUtils azureUtils;

    public List<CloudResource> getNetworkResources(List<CloudResource> resources) {
        return resources.stream()
                .filter(cloudResource -> List.of(
                        ResourceType.AZURE_SUBNET,
                        ResourceType.AZURE_NETWORK,
                        ResourceType.AZURE_RESOURCE_GROUP)
                        .contains(cloudResource.getType()))
                .collect(Collectors.toList());
    }

    public List<CloudResource> getDeploymentCloudResources(Deployment templateDeployment) {
        PagedList<DeploymentOperation> operations = templateDeployment.deploymentOperations().list();
        List<CloudResource> resourceList = operations.stream()
                .filter(Predicate.not(Predicate.isEqual(null)))
                .filter(deploymentOperation -> Objects.nonNull(deploymentOperation.targetResource())
                        && StringUtils.isNotBlank(deploymentOperation.provisioningState()))
                .map(deploymentOperation ->
                        convert(deploymentOperation.targetResource(),
                                convertProvisioningState(deploymentOperation.provisioningState())))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        LOGGER.debug("Template deployment related cloud resource list: {}", resourceList);
        return resourceList;
    }

    @SuppressWarnings("checkstyle:CyclomaticComplexity")
    private CommonStatus convertProvisioningState(String provisioningState) {
        CommonStatus status;
        switch (provisioningState.toLowerCase()) {
            case "succeeded":
            case "created":
                status = CommonStatus.CREATED;
                break;
            case "accepted":
            case "creating":
            case "running":
            case "registering":
                status = CommonStatus.REQUESTED;
                break;
            case "failed":
            case "canceled":
            case "deleting":
            case "deleted":
            case "notspecified":
            default:
                status = CommonStatus.FAILED;
        }
        return status;
    }

    @SuppressWarnings("checkstyle:CyclomaticComplexity")
    private CloudResource convert(TargetResource targetResource, CommonStatus commonStatus) {

        CloudResource.Builder cloudResourceBuilder = CloudResource.builder();
        switch (targetResource.resourceType()) {
            case "Microsoft.Compute/availabilitySets":
                cloudResourceBuilder.type(ResourceType.AZURE_AVAILABILITY_SET);
                break;
            case "Microsoft.Compute/virtualMachines":
                cloudResourceBuilder.type(ResourceType.AZURE_INSTANCE);
                cloudResourceBuilder.instanceId(targetResource.resourceName());
                break;
            case "Microsoft.Network/networkSecurityGroups":
                cloudResourceBuilder.type(ResourceType.AZURE_SECURITY_GROUP);
                break;
            case "Microsoft.Network/publicIPAddresses":
                cloudResourceBuilder.type(ResourceType.AZURE_PUBLIC_IP);
                break;
            case "Microsoft.Network/networkInterfaces":
                cloudResourceBuilder.type(ResourceType.AZURE_NETWORK_INTERFACE);
                break;
            case "Microsoft.Network/virtualNetworks":
                cloudResourceBuilder.type(ResourceType.AZURE_NETWORK);
                break;
            case "Microsoft.Network/privateEndpoints":
                cloudResourceBuilder.type(ResourceType.AZURE_PRIVATE_ENDPOINT);
                break;
            case "Microsoft.DBforPostgreSQL/servers":
                cloudResourceBuilder.type(ResourceType.AZURE_DATABASE);
                break;
            case "Microsoft.DBforPostgreSQL/servers/securityAlertPolicies":
                cloudResourceBuilder.type(ResourceType.AZURE_DATABASE_SECURITY_ALERT_POLICY);
                break;
            case "Microsoft.Compute/images":
                cloudResourceBuilder.type(ResourceType.AZURE_MANAGED_IMAGE);
                break;
            case "Microsoft.Compute/disks":
                cloudResourceBuilder.type(ResourceType.AZURE_DISK);
                break;
            case "Microsoft.Storage/storageAccounts":
                cloudResourceBuilder.type(ResourceType.AZURE_STORAGE);
                break;
            case "Microsoft.Network/privateDnsZones":
                cloudResourceBuilder.type(ResourceType.AZURE_PRIVATE_DNS_ZONE);
                break;
            case "Microsoft.Network/privateEndpoints/privateDnsZoneGroups":
                cloudResourceBuilder.type(ResourceType.AZURE_DNS_ZONE_GROUP);
                break;
            case "Microsoft.Network/privateDnsZones/virtualNetworkLinks":
                cloudResourceBuilder.type(ResourceType.AZURE_VIRTUAL_NETWORK_LINK);
                break;
            case "Microsoft.Network/loadBalancers":
                cloudResourceBuilder.type(ResourceType.AZURE_LOAD_BALANCER);
                break;
            default:
                LOGGER.info("Unknown resource type {}", targetResource.resourceType());
                return null;
        }
        CloudResource cloudResource = cloudResourceBuilder
                .name(targetResource.resourceName())
                .reference(targetResource.id())
                .status(CommonStatus.CREATED)
                .persistent(true)
                .status(commonStatus)
                .build();
        LOGGER.debug("Cloud resource built {}, original Azure resource name: {}, type: {}",
                cloudResource,
                targetResource.resourceName(),
                targetResource.resourceType());
        return cloudResource;
    }

    public List<CloudResource> getInstanceCloudResources(String stackName, List<CloudResource> cloudResourceList, List<Group> groups,
            String resourceGroupName) {

        List<CloudResource> vmResourceList = new ArrayList<>();
        cloudResourceList
                .stream()
                .filter(cloudResource -> cloudResource.getType().equals(ResourceType.AZURE_INSTANCE))
                .forEach(instance -> groups
                        .stream()
                        .map(Group::getInstances)
                        .forEach(cloudInstances -> cloudInstances
                                .stream()
                                .filter(cloudInstance -> cloudInstance.getTemplate().getStatus().equals(InstanceStatus.CREATE_REQUESTED))
                                .filter(cloudInstance -> instance.getName().equalsIgnoreCase(
                                        azureUtils.getPrivateInstanceId(stackName,
                                                cloudInstance.getTemplate().getGroupName(),
                                                Long.toString(cloudInstance.getTemplate().getPrivateId()))))
                                .peek(cloudInstance -> LOGGER.debug("The following resource is categorized as VM instance {}", cloudInstance.toString()))
                                .forEach(filteredInstance ->
                                        vmResourceList.add(buildVm(instance, filteredInstance.getTemplate().getPrivateId(),
                                                filteredInstance.getTemplate().getGroupName(), resourceGroupName))
                                )
                        )
                );
        return vmResourceList;
    }

    public CloudResource buildCloudResource(String name, String id, ResourceType type) {
        LOGGER.debug("{} {} is being created with id {}", type.name(), name, id);
        return CloudResource.builder()
                .name(name)
                .status(CommonStatus.CREATED)
                .persistent(true)
                .reference(id)
                .type(type)
                .build();
    }

    private CloudResource buildVm(CloudResource sourceResource, Long privateId, String instanceGroupName, String resourceGroupName) {
        return CloudResource.builder()
                .type(sourceResource.getType())
                .instanceId(sourceResource.getInstanceId())
                .name(sourceResource.getName())
                .group(instanceGroupName)
                .status(sourceResource.getStatus())
                .persistent(sourceResource.isPersistent())
                .params(Map.of(RESOURCE_GROUP_NAME_PARAMETER, resourceGroupName, PRIVATE_ID, privateId))
                .build();
    }

    // OS disks are not part of the deployment as separate deployment operations meaning they should be collected
    private CloudResource collectOsDisk(String instanceId, VirtualMachine virtualMachine) {
        StorageProfile storageProfile = virtualMachine.storageProfile();
        OSDisk osDisk = storageProfile.osDisk();
        LOGGER.debug("OS disk {} found for VM {}", osDisk.name(), virtualMachine.name());
        return CloudResource.builder()
                .name(osDisk.name())
                .instanceId(instanceId)
                .status(CommonStatus.CREATED)
                .persistent(true)
                .reference(osDisk.managedDisk().id())
                .type(ResourceType.AZURE_DISK)
                .build();
    }

    public List<CloudResource> getAttachedOsDiskResources(List<CloudResource> instanceList,
            String resourceGroupName, AzureClient client) {

        List<CloudResource> osDiskList = new ArrayList<>();
        PagedList<VirtualMachine> virtualMachines = client.getVirtualMachines(resourceGroupName);
        virtualMachines.loadAll();

        instanceList.forEach(vm -> {
            Optional<VirtualMachine> matchingAzureVmOptional = virtualMachines
                    .stream()
                    .filter(azureVirtualMachine -> vm.getName().equals(azureVirtualMachine.name()))
                    .findFirst();
            matchingAzureVmOptional.ifPresentOrElse(
                    matchingAzureVm ->
                            osDiskList.add(collectOsDisk(vm.getInstanceId(), matchingAzureVm)),
                    () -> LOGGER.warn("No Azure VM metadata found for the VM: " + vm.getInstanceId()));
                }
        );
        LOGGER.debug("The following OS disks have been found: {}", osDiskList);
        return osDiskList;
    }

    public void saveCloudResources(PersistenceNotifier notifier, CloudContext cloudContext, List<CloudResource> cloudResources) {
        cloudResources.forEach(cloudResource -> notifier.notifyAllocation(cloudResource, cloudContext));
    }

    public void deleteCloudResources(PersistenceNotifier notifier, CloudContext cloudContext, List<CloudResource> cloudResources) {
        cloudResources.forEach(cloudResource -> notifier.notifyDeletion(cloudResource, cloudContext));
    }

    public List<CloudResource> collectAndSaveNetworkAndSubnet(String resourceGroupName, String virtualNetwork, PersistenceNotifier notifier,
            CloudContext cloudContext, List<String> subnetNameList, String networkName, AzureClient client) {
        List<CloudResource> resources = new ArrayList<>();

        if (subnetNameList.isEmpty()) {
            Optional<Subnet> first = client.getSubnets(resourceGroupName, virtualNetwork).values().stream().findFirst();
            if (first.isPresent()) {
                Subnet subnet = first.get();
                String subnetName = subnet.name();
                subnetNameList.add(subnetName);
                networkName = subnet.parent().name();
            }
        }
        CloudResource resourceGroupResource = CloudResource.builder().
                name(resourceGroupName).
                type(ResourceType.AZURE_RESOURCE_GROUP).
                build();
        resources.add(resourceGroupResource);
        notifier.notifyAllocation(resourceGroupResource, cloudContext);

        CloudResource networkResource = CloudResource.builder().
                name(networkName).
                type(ResourceType.AZURE_NETWORK).
                build();
        resources.add(networkResource);
        notifier.notifyAllocation(networkResource, cloudContext);
        for (String subnetName : subnetNameList) {
            CloudResource subnetResource = CloudResource.builder().
                    name(subnetName).
                    type(ResourceType.AZURE_SUBNET).
                    build();
            resources.add(subnetResource);
            notifier.notifyAllocation(subnetResource, cloudContext);
        }
        return resources;
    }
}
