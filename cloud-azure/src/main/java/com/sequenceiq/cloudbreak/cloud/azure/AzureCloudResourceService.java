package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.RESOURCE_GROUP_NAME_PARAMETER;
import static com.sequenceiq.cloudbreak.cloud.model.CloudResource.PRIVATE_ID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.azure.resourcemanager.compute.models.OSDisk;
import com.azure.resourcemanager.compute.models.StorageProfile;
import com.azure.resourcemanager.compute.models.VirtualMachine;
import com.azure.resourcemanager.network.models.Subnet;
import com.azure.resourcemanager.resources.models.Deployment;
import com.azure.resourcemanager.resources.models.TargetResource;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureListResult;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureListResultFactory;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@Component
public class AzureCloudResourceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureCloudResourceService.class);

    @Inject
    private AzureUtils azureUtils;

    @Inject
    private AzureListResultFactory azureListResultFactory;

    public List<CloudResource> getNetworkResources(List<CloudResource> resources) {
        return resources.stream()
                .filter(cloudResource -> List.of(
                                ResourceType.AZURE_SUBNET,
                                ResourceType.AZURE_NETWORK,
                                ResourceType.AZURE_RESOURCE_GROUP)
                        .contains(cloudResource.getType()))
                .collect(Collectors.toList());
    }

    public List<ResourceType> getPrivateEndpointRdsResourceTypes() {
        return List.of(ResourceType.AZURE_DNS_ZONE_GROUP,
                ResourceType.AZURE_PRIVATE_ENDPOINT);
    }

    public List<CloudResource> getDeploymentCloudResources(Deployment templateDeployment) {
        List<CloudResource> resourceList = azureListResultFactory.list(templateDeployment.deploymentOperations())
                .getStream()
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
        return switch (provisioningState.toLowerCase(Locale.ROOT)) {
            case "succeeded", "created" -> CommonStatus.CREATED;
            case "accepted", "creating", "running", "registering" -> CommonStatus.REQUESTED;
            default -> CommonStatus.FAILED;
        };
    }

    @SuppressWarnings("checkstyle:CyclomaticComplexity")
    private CloudResource convert(TargetResource targetResource, CommonStatus commonStatus) {

        CloudResource.Builder cloudResourceBuilder = CloudResource.builder();
        switch (targetResource.resourceType()) {
            case "Microsoft.Compute/availabilitySets":
                cloudResourceBuilder.withType(ResourceType.AZURE_AVAILABILITY_SET);
                break;
            case "Microsoft.Compute/virtualMachines":
                cloudResourceBuilder.withType(ResourceType.AZURE_INSTANCE);
                cloudResourceBuilder.withInstanceId(targetResource.resourceName());
                break;
            case "Microsoft.Network/networkSecurityGroups":
                cloudResourceBuilder.withType(ResourceType.AZURE_SECURITY_GROUP);
                break;
            case "Microsoft.Network/publicIPAddresses":
                cloudResourceBuilder.withType(ResourceType.AZURE_PUBLIC_IP);
                break;
            case "Microsoft.Network/networkInterfaces":
                cloudResourceBuilder.withType(ResourceType.AZURE_NETWORK_INTERFACE);
                break;
            case "Microsoft.Network/virtualNetworks":
                cloudResourceBuilder.withType(ResourceType.AZURE_NETWORK);
                break;
            case "Microsoft.Network/privateEndpoints":
                cloudResourceBuilder.withType(ResourceType.AZURE_PRIVATE_ENDPOINT);
                break;
            case "Microsoft.DBforPostgreSQL/servers":
            case "Microsoft.DBforPostgreSQL/flexibleServers":
                cloudResourceBuilder.withType(ResourceType.AZURE_DATABASE);
                break;
            case "Microsoft.DBforPostgreSQL/servers/securityAlertPolicies":
                cloudResourceBuilder.withType(ResourceType.AZURE_DATABASE_SECURITY_ALERT_POLICY);
                break;
            case "Microsoft.Compute/images":
                cloudResourceBuilder.withType(ResourceType.AZURE_MANAGED_IMAGE);
                break;
            case "Microsoft.Compute/disks":
                cloudResourceBuilder.withType(ResourceType.AZURE_DISK);
                break;
            case "Microsoft.Storage/storageAccounts":
                cloudResourceBuilder.withType(ResourceType.AZURE_STORAGE);
                break;
            case "Microsoft.Network/privateDnsZones":
                cloudResourceBuilder.withType(ResourceType.AZURE_PRIVATE_DNS_ZONE);
                break;
            case "Microsoft.Network/privateEndpoints/privateDnsZoneGroups":
                cloudResourceBuilder.withType(ResourceType.AZURE_DNS_ZONE_GROUP);
                break;
            case "Microsoft.Network/privateDnsZones/virtualNetworkLinks":
                cloudResourceBuilder.withType(ResourceType.AZURE_VIRTUAL_NETWORK_LINK);
                break;
            case "Microsoft.Network/loadBalancers":
                cloudResourceBuilder.withType(ResourceType.AZURE_LOAD_BALANCER);
                break;
            default:
                LOGGER.info("Unknown resource type {}", targetResource.resourceType());
                return null;
        }
        CloudResource cloudResource = cloudResourceBuilder
                .withName(targetResource.resourceName())
                .withReference(targetResource.id())
                .withStatus(CommonStatus.CREATED)
                .withPersistent(true)
                .withStatus(commonStatus)
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
                        .forEach(cloudInstances -> {
                                    List<CloudInstance> vmInstances = cloudInstances
                                            .stream()
                                            .filter(cloudInstance -> cloudInstance.getTemplate().getStatus().equals(InstanceStatus.CREATE_REQUESTED))
                                            .filter(cloudInstance -> instance.getName().equalsIgnoreCase(
                                                    azureUtils.getFullInstanceId(stackName,
                                                            cloudInstance.getTemplate().getGroupName(),
                                                            Long.toString(cloudInstance.getTemplate().getPrivateId()),
                                                            cloudInstance.getDbIdOrDefaultIfNotExists())))
                                            .collect(Collectors.toList());
                                    LOGGER.info("The following resources are categorized as VM instance: {}", vmInstances);
                                    vmInstances.forEach(filteredInstance ->
                                            vmResourceList.add(buildVm(instance, filteredInstance.getTemplate().getPrivateId(),
                                                    filteredInstance.getTemplate().getGroupName(), resourceGroupName)));
                                }
                        )
                );
        return vmResourceList;
    }

    public CloudResource buildCloudResource(String name, String id, ResourceType type) {
        LOGGER.debug("{} {} is being created with id {}", type.name(), name, id);
        return CloudResource.builder()
                .withName(name)
                .withStatus(CommonStatus.CREATED)
                .withPersistent(true)
                .withReference(id)
                .withType(type)
                .build();
    }

    private CloudResource buildVm(CloudResource sourceResource, Long privateId, String instanceGroupName, String resourceGroupName) {
        return CloudResource.builder()
                .withType(sourceResource.getType())
                .withInstanceId(sourceResource.getInstanceId())
                .withName(sourceResource.getName())
                .withGroup(instanceGroupName)
                .withStatus(sourceResource.getStatus())
                .withPersistent(sourceResource.isPersistent())
                .withParameters(Map.of(RESOURCE_GROUP_NAME_PARAMETER, resourceGroupName, PRIVATE_ID, privateId))
                .build();
    }

    // OS disks are not part of the deployment as separate deployment operations meaning they should be collected
    private CloudResource collectOsDisk(String instanceId, VirtualMachine virtualMachine, String availabilityZone, String instanceGroup) {
        StorageProfile storageProfile = virtualMachine.storageProfile();
        OSDisk osDisk = storageProfile.osDisk();
        LOGGER.debug("OS disk {} found for VM {} in host group {}", osDisk.name(), virtualMachine.name(), instanceGroup);
        return CloudResource.builder()
                .withName(osDisk.name())
                .withInstanceId(instanceId)
                .withStatus(CommonStatus.CREATED)
                .withPersistent(true)
                .withReference(osDisk.managedDisk().id())
                .withType(ResourceType.AZURE_DISK)
                .withGroup(instanceGroup)
                .withParameters(Map.of(CloudResource.ATTRIBUTES, new VolumeSetAttributes.Builder()
                                .withAvailabilityZone(availabilityZone)
                                .withDeleteOnTermination(Boolean.TRUE)
                                .withVolumes(List.of(new VolumeSetAttributes.Volume(osDisk.name(), null, osDisk.diskSizeGB(),
                                        osDisk.managedDisk().storageAccountType().toString(), CloudVolumeUsageType.GENERAL)))
                                .build()))
                .build();
    }

    public List<CloudResource> getAttachedOsDiskResources(List<CloudResource> instanceList, String resourceGroupName, AzureClient client) {
        AzureListResult<VirtualMachine> virtualMachines = client.getVirtualMachines(resourceGroupName);
        Map<String, CloudResource> instancesByName = new HashMap<>();
        instanceList.forEach(instance -> {
            if (Objects.nonNull(instance.getName())) {
                instancesByName.put(instance.getName(), instance);
            }
        });
        List<CloudResource> osDiskList = virtualMachines
                .getStream()
                .takeWhile(instance -> !instancesByName.isEmpty())
                .reduce(new ArrayList<>(), (acc, azureInstance) -> {
                    if (instancesByName.containsKey(azureInstance.name())) {
                        CloudResource vm = instancesByName.remove(azureInstance.name());
                        acc.add(collectOsDisk(vm.getInstanceId(), azureInstance, vm.getAvailabilityZone(), vm.getGroup()));
                    }
                    return acc;
                }, this::mergeLists);
        LOGGER.warn("No Azure VM metadata found for VMs: {}", instancesByName.keySet());
        LOGGER.debug("The following OS disks have been found: {}", osDiskList);
        return osDiskList;
    }

    private ArrayList<CloudResource> mergeLists(Collection<CloudResource> listOne, Collection<CloudResource> listTwo) {
        ArrayList<CloudResource> result = new ArrayList<>();
        result.addAll(listOne);
        result.addAll(listTwo);
        return result;
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
                withName(resourceGroupName).
                withType(ResourceType.AZURE_RESOURCE_GROUP).
                build();
        resources.add(resourceGroupResource);
        notifier.notifyAllocation(resourceGroupResource, cloudContext);

        CloudResource networkResource = CloudResource.builder().
                withName(networkName).
                withType(ResourceType.AZURE_NETWORK).
                build();
        resources.add(networkResource);
        notifier.notifyAllocation(networkResource, cloudContext);
        for (String subnetName : subnetNameList) {
            CloudResource subnetResource = CloudResource.builder().
                    withName(subnetName).
                    withType(ResourceType.AZURE_SUBNET).
                    build();
            resources.add(subnetResource);
            notifier.notifyAllocation(subnetResource, cloudContext);
        }
        return resources;
    }
}
