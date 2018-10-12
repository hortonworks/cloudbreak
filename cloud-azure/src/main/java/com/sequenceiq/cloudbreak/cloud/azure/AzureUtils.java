package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.cloudbreak.cloud.azure.AzureResourceConnector.RESOURCE_GROUP_NAME;
import static org.apache.commons.lang3.StringUtils.isNoneEmpty;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Splitter;
import com.microsoft.azure.management.network.NetworkSecurityGroup;
import com.microsoft.azure.management.network.NetworkSecurityRule;
import com.microsoft.azure.management.network.Subnet;
import com.microsoft.azure.management.resources.Deployment;
import com.microsoft.azure.management.resources.DeploymentOperation;
import com.microsoft.azure.management.resources.DeploymentOperations;
import com.microsoft.azure.management.resources.fluentcore.arm.models.HasId;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.status.AzureStackStatus;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

@Component
public class AzureUtils {

    public static final int NOT_FOUND = 404;

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureUtils.class);

    private static final String RG_NAME = "resourceGroupName";

    private static final String NETWORK_ID = "networkId";

    private static final String NO_PUBLIC_IP = "noPublicIp";

    private static final String NO_FIREWALL_RULES = "noFirewallRules";

    private static final int PORT_22 = 22;

    private static final int PORT_443 = 443;

    private static final int PORT_RANGE_NUM = 2;

    private static final int RG_PART = 4;

    private static final int ID_SEGMENTS = 9;

    private static final int SEC_GROUP_PART = 8;

    private static final int HOST_GROUP_LENGTH = 3;

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
        LOGGER.info("Azure stack status of: {}  is: {}", resource.getName(), status);
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

    public String getResourceGroupName(CloudContext cloudContext) {
        return getStackName(cloudContext);
    }

    public String getResourceGroupName(CloudContext cloudContext, CloudStack cloudStack) {
        return cloudStack.getParameters().getOrDefault(RESOURCE_GROUP_NAME, getResourceGroupName(cloudContext));
    }

    public String getResourceGroupName(CloudContext cloudContext, DynamicModel dynamicModel) {
        return dynamicModel.getParameters().getOrDefault(RESOURCE_GROUP_NAME, getResourceGroupName(cloudContext)).toString();
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
            return Collections.emptyList();
        }
        return Arrays.asList(network.getStringParameter(CloudInstance.SUBNET_ID).split(","));
    }

    public void validateSubnetRules(AzureClient client, Network network) {
        if (isExistingNetwork(network)) {
            String resourceGroupName = getCustomResourceGroupName(network);
            String networkId = getCustomNetworkId(network);
            Collection<String> subnetIds = getCustomSubnetIds(network);
            for (String subnetId : subnetIds) {
                try {
                    Subnet subnet = client.getSubnetProperties(resourceGroupName, networkId, subnetId);
                    if (subnet == null) {
                        throw new CloudConnectorException(
                                String.format("Subnet [%s] does not found with resourceGroupName [%s] and network [%s]", subnetId, resourceGroupName, networkId)
                        );
                    }
                    NetworkSecurityGroup networkSecurityGroup = subnet.getNetworkSecurityGroup();
                    if (networkSecurityGroup != null) {
                        validateSecurityGroup(client, networkSecurityGroup);
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
            String volumeType = template.getVolumeType();
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

    private void validateSecurityGroup(AzureClient client, HasId networkSecurityGroup) {
        String securityGroupId = networkSecurityGroup.id();
        String[] parts = securityGroupId.split("/");
        if (parts.length != ID_SEGMENTS) {
            LOGGER.info("Cannot get the security group's properties, id: {}", securityGroupId);
            return;
        }
        try {
            NetworkSecurityGroup securityGroup = client.getSecurityGroupProperties(parts[RG_PART], parts[SEC_GROUP_PART]);
            LOGGER.info("Retrieved security group properties: {}", securityGroup);
            Map<String, NetworkSecurityRule> securityRules = securityGroup.securityRules();
            boolean port22Found = false;
            boolean port443Found = false;

            for (NetworkSecurityRule securityRule : securityRules.values()) {
                if (isValidInboundRule(securityRule)) {
                    String destinationPortRange = securityRule.destinationPortRange();
                    if ("*".equals(destinationPortRange)) {
                        return;
                    }
                    String[] range = destinationPortRange.split("-");
                    port443Found = port443Found || isPortFound(PORT_443, range);
                    port22Found = port22Found || isPortFound(PORT_22, range);
                    if (port22Found && port443Found) {
                        return;
                    }
                }
            }
        } catch (RuntimeException e) {
            throw new CloudConnectorException("Validating security group failed.", e);
        }
        throw new CloudConnectorException("The specified subnet's security group does not allow traffic for port 22 and/or 443");
    }

    private boolean isValidInboundRule(NetworkSecurityRule securityRule) {
        String protocol = securityRule.protocol().toString().toLowerCase();
        String access = securityRule.access().toString().toLowerCase();
        String direction = securityRule.direction().toString().toLowerCase();
        return "inbound".equals(direction)
                && ("tcp".equals(protocol) || "*".equals(protocol))
                && "allow".equals(access);
    }

    private boolean isPortFound(int port, String[] destinationPortRange) {
        if (destinationPortRange.length == PORT_RANGE_NUM) {
            return isPortInRange(port, destinationPortRange);
        }
        return isPortMatch(port, destinationPortRange[0]);
    }

    private boolean isPortInRange(int port, String[] range) {
        return Integer.parseInt(range[0]) <= port && Integer.parseInt(range[1]) >= port;
    }

    private boolean isPortMatch(int port, String destinationPortRange) {
        return port == Integer.parseInt(destinationPortRange);
    }

}
