package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.cloudbreak.cloud.azure.AzureResourceConnector.RESOURCE_GROUP_NAME;
import static org.apache.commons.lang3.StringUtils.isNoneEmpty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Splitter;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.network.Subnet;
import com.microsoft.azure.management.resources.Deployment;
import com.microsoft.azure.management.resources.DeploymentOperation;
import com.microsoft.azure.management.resources.DeploymentOperations;
import com.microsoft.azure.management.resources.TargetResource;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.status.AzureStackStatus;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel;
import com.sequenceiq.cloudbreak.common.type.CommonStatus;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

@Component
public class AzureUtils {
    private static final String RG_NAME = "resourceGroupName";

    private static final String NETWORK_ID = "networkId";

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureUtils.class);

    private static final String NO_PUBLIC_IP = "noPublicIp";

    private static final String NO_FIREWALL_RULES = "noFirewallRules";

    private static final int HOST_GROUP_LENGTH = 3;

    private static final String MICROSOFT_COMPUTE_VIRTUAL_MACHINES = "Microsoft.Compute/virtualMachines";

    @Value("${cb.max.azure.resource.name.length:}")
    private int maxResourceNameLength;

    @Inject
    private AzurePremiumValidatorService azurePremiumValidatorService;

    public CloudResource getTemplateResource(Iterable<CloudResource> resourceList) {
        for (CloudResource resource : resourceList) {
            if (resource.getType() == ResourceType.ARM_TEMPLATE) {
                return resource;
            }
        }
        throw new CloudConnectorException(String.format("No resource found: %s", ResourceType.ARM_TEMPLATE));
    }

    public static String getGroupName(String group) {
        String shortened = WordUtils.initials(group.replaceAll("_", " ")).toLowerCase();
        return shortened.length() <= HOST_GROUP_LENGTH ? shortened : shortened.substring(0, HOST_GROUP_LENGTH);
    }

    public static void removeBlankSpace(StringBuilder sb) {
        int j = 0;
        for (int i = 0; i < sb.length(); i++) {
            if (!Character.isWhitespace(sb.charAt(i))) {
                sb.setCharAt(j++, sb.charAt(i));
            }
        }
        sb.delete(j, sb.length());
    }

    public String getLoadBalancerId(String stackName) {
        return String.format("%s%s", stackName, "lb");
    }

    public String getPrivateInstanceId(String stackName, String groupName, String privateId) {
        return String.format("%s%s%s", stackName, getGroupName(groupName), privateId);
    }

    public String getStackName(CloudContext cloudContext) {
        return Splitter.fixedLength(maxResourceNameLength - cloudContext.getId().toString().length())
                .splitToList(cloudContext.getName()).get(0) + cloudContext.getId();
    }

    public CloudResourceStatus getTemplateStatus(CloudResource resource, Deployment templateDeployment, AzureClient access, String stackName) {
        String status = templateDeployment.provisioningState();
        LOGGER.debug("Azure stack status of: {}  is: {}", resource.getName(), status);
        ResourceStatus resourceStatus = AzureStackStatus.mapResourceStatus(status);
        CloudResourceStatus armResourceStatus = null;

        if (ResourceStatus.FAILED.equals(resourceStatus)) {
            LOGGER.debug("Cloud resource status: {}", resourceStatus);
            try {
                DeploymentOperations templateDeploymentOperations = access.getTemplateDeploymentOperations(stackName, stackName);
                for (DeploymentOperation deploymentOperation : templateDeploymentOperations.list()) {

                    if ("Failed".equals(deploymentOperation.provisioningState())) {
                        String statusMessage = (String) deploymentOperation.statusMessage();
                        armResourceStatus = new CloudResourceStatus(resource, AzureStackStatus.mapResourceStatus(status), statusMessage);
                        break;
                    }
                }
            } catch (RuntimeException e) {
                armResourceStatus = new CloudResourceStatus(resource, AzureStackStatus.mapResourceStatus(status), e.getMessage());
            }
        } else {
            LOGGER.debug("Cloud resource status: {}", resourceStatus);
            armResourceStatus = new CloudResourceStatus(resource, AzureStackStatus.mapResourceStatus(status));
        }
        return armResourceStatus;
    }

    private String getDefaultResourceGroupName(CloudContext cloudContext) {
        return getStackName(cloudContext);
    }

    public String getResourceGroupName(CloudContext cloudContext, CloudStack cloudStack) {
        return cloudStack.getParameters().getOrDefault(RESOURCE_GROUP_NAME, getDefaultResourceGroupName(cloudContext));
    }

    public String getResourceGroupName(CloudContext cloudContext, DynamicModel dynamicModel) {
        return dynamicModel.getParameters().getOrDefault(RESOURCE_GROUP_NAME, getDefaultResourceGroupName(cloudContext)).toString();
    }

    public boolean isExistingNetwork(Network network) {
        return isNoneEmpty(getCustomNetworkId(network)) && isNoneEmpty(getCustomResourceGroupName(network)) && isListNotEmpty(getCustomSubnetIds(network));
    }

    private boolean isListNotEmpty(Collection<String> c) {
        return c != null && !c.isEmpty();
    }

    public boolean isPrivateIp(Network network) {
        return network.getParameters().containsKey(NO_PUBLIC_IP) ? network.getParameter(NO_PUBLIC_IP, Boolean.class) : false;
    }

    public boolean isNoSecurityGroups(Network network) {
        return network.getParameters().containsKey(NO_FIREWALL_RULES) ? network.getParameter(NO_FIREWALL_RULES, Boolean.class) : false;
    }

    public static List<CloudInstance> getInstanceList(CloudStack stack) {
        return stack.getGroups().stream().flatMap(group -> group.getInstances().stream()).collect(Collectors.toList());
    }

    public static boolean hasManagedDisk(CloudStack stack) {
        List<CloudInstance> instanceList = getInstanceList(stack);
        return instanceList.stream().anyMatch(cloudInstance -> Boolean.TRUE.equals(cloudInstance.getTemplate().getParameter("managedDisk", Boolean.class)));
    }

    public static boolean hasUnmanagedDisk(CloudStack stack) {
        List<CloudInstance> instanceList = getInstanceList(stack);
        return instanceList.stream().anyMatch(cloudInstance -> !Boolean.TRUE.equals(cloudInstance.getTemplate().getParameter("managedDisk", Boolean.class)));
    }

    public String getCustomNetworkId(Network network) {
        return network.getStringParameter(NETWORK_ID);
    }

    public String getCustomResourceGroupName(Network network) {
        return network.getStringParameter(RG_NAME);
    }

    public List<String> getCustomSubnetIds(Network network) {
        String subnetIds = network.getStringParameter(CloudInstance.SUBNET_ID);
        if (StringUtils.isBlank(subnetIds)) {
            return new ArrayList<>();
        }
        return Arrays.asList(subnetIds.split(","));
    }

    public void validateSubnet(AzureClient client, Network network) {
        if (isExistingNetwork(network)) {
            String resourceGroupName = getCustomResourceGroupName(network);
            String networkId = getCustomNetworkId(network);
            Collection<String> subnetIds = getCustomSubnetIds(network);
            for (String subnetId : subnetIds) {
                try {
                    Subnet subnet = client.getSubnetProperties(resourceGroupName, networkId, subnetId);
                    if (subnet == null) {
                        throw new CloudConnectorException(
                                String.format("Subnet [%s] is not found in resource group [%s] and network [%s]", subnetId, resourceGroupName, networkId)
                        );
                    }
                } catch (RuntimeException e) {
                    throw new CloudConnectorException("Subnet validation failed, cause: " + e.getMessage(), e);
                }
            }
        }
    }

    public void validateStorageType(CloudStack stack) {
        for (Group group : stack.getGroups()) {
            InstanceTemplate template = group.getReferenceInstanceConfiguration().getTemplate();
            String flavor = template.getFlavor();
            String volumeType = template.getVolumes().get(0).getType();
            AzureDiskType diskType = AzureDiskType.getByValue(volumeType);
            validateStorageTypeForGroup(diskType, flavor);
        }
    }

    public void validateStorageTypeForGroup(AzureDiskType diskType, String flavor) {
        if (azurePremiumValidatorService.premiumDiskTypeConfigured(diskType)) {
            if (!azurePremiumValidatorService.validPremiumConfiguration(flavor)) {
                throw new CloudConnectorException("Only the DS instance types supports the premium storage.");
            }
        }
    }

    public List<CloudResource> getInstanceCloudResources(CloudContext cc,
            Deployment templateDeployment, List<Group> groups) {
        PagedList<DeploymentOperation> operations = templateDeployment.deploymentOperations().list();
        List<CloudResource> cloudResourceList = new ArrayList<>();
        for (DeploymentOperation operation : operations) {
            TargetResource resource = operation.targetResource();
            if (Objects.nonNull(resource) && resource.resourceType().equals(MICROSOFT_COMPUTE_VIRTUAL_MACHINES)) {
                String vmName = operation.targetResource().resourceName();
                String resourceGroupNm = templateDeployment.resourceGroupName();
                for (Group group : groups) {
                    for (CloudInstance instance : group.getInstances()) {
                        String id = getPrivateInstanceId(
                                getStackName(cc), group.getName(), Long.toString(instance.getTemplate().getPrivateId()));
                        if (vmName.equals(id) && instance.getTemplate().getStatus().equals(InstanceStatus.CREATE_REQUESTED)) {
                            Map<String, Object> paramsMap = new HashMap<>();
                            paramsMap.put(RESOURCE_GROUP_NAME, resourceGroupNm);
                            CloudResource vm = CloudResource.builder()
                                    .type(ResourceType.AZURE_INSTANCE)
                                    .instanceId(id)
                                    .name(id)
                                    .group(group.getName())
                                    .status(CommonStatus.CREATED)
                                    .persistent(false)
                                    .params(paramsMap)
                                    .build();
                            cloudResourceList.add(vm);
                            break;
                        }
                    }
                }
            }
        }
        return cloudResourceList;
    }
}
