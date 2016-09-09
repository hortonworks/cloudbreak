package com.sequenceiq.cloudbreak.cloud.arm;

import static org.apache.commons.lang3.StringUtils.isNoneEmpty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Splitter;
import com.sequenceiq.cloud.azure.client.AzureRMClient;
import com.sequenceiq.cloudbreak.cloud.arm.status.ArmStackStatus;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

import groovyx.net.http.HttpResponseException;

@Component
public class ArmUtils {

    public static final int NOT_FOUND = 404;

    private static final Logger LOGGER = LoggerFactory.getLogger(ArmUtils.class);
    private static final String RG_NAME = "resourceGroupName";
    private static final String SUBNET_ID = "subnetId";
    private static final String NETWORK_ID = "networkId";
    private static final int PORT_22 = 22;
    private static final int PORT_443 = 443;
    private static final int PORT_RANGE_NUM = 2;
    private static final int RG_PART = 4;
    private static final int ID_SEGMENTS = 9;
    private static final int SEC_GROUP_PART = 8;
    private static final int HOST_GROUP_LENGTH = 3;

    @Value("${cb.max.azure.resource.name.length:}")
    private int maxResourceNameLength;

    public CloudResource getTemplateResource(List<CloudResource> resourceList) {
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

    public String getPrivateInstanceId(String stackName, String groupName, String privateId) {
        return String.format("%s%s%s", stackName, getGroupName(groupName), privateId);
    }

    public String getStackName(CloudContext cloudContext) {
        return Splitter.fixedLength(maxResourceNameLength - cloudContext.getId().toString().length())
                .splitToList(cloudContext.getName()).get(0) + cloudContext.getId();
    }

    public String getLoadBalancerId(String stackName) {
        return String.format("%s%s", stackName, "lb");
    }

    public CloudResourceStatus templateStatus(CloudResource resource, Map<String, Object> templateDeployment, AzureRMClient access, String stackName) {
        String status = ((Map) templateDeployment.get("properties")).get("provisioningState").toString();
        LOGGER.info("Arm stack status of: {}  is: {}", resource.getName(), status);
        ResourceStatus resourceStatus = ArmStackStatus.mapResourceStatus(status);
        CloudResourceStatus armResourceStatus = null;
        if (ResourceStatus.FAILED.equals(resourceStatus)) {
            LOGGER.debug("Cloud resource status: {}", resourceStatus);
            try {
                Map<String, Object> templateDeploymentOperations = access.getTemplateDeploymentOperations(stackName, stackName);
                List<Map> value = (ArrayList<Map>) templateDeploymentOperations.get("value");
                for (Map map : value) {
                    Map properties = (Map) map.get("properties");
                    if ("Failed".equals(properties.get("provisioningState").toString())) {
                        Map statusMessage = (Map) properties.get("statusMessage");
                        Map error = (Map) statusMessage.get("error");
                        String message = error.get("message").toString();
                        armResourceStatus = new CloudResourceStatus(resource, ArmStackStatus.mapResourceStatus(status), message);
                        break;
                    }
                }

            } catch (HttpResponseException e) {
                armResourceStatus = new CloudResourceStatus(resource, ArmStackStatus.mapResourceStatus(status), e.getResponse().getData().toString());
            } catch (Exception e) {
                armResourceStatus = new CloudResourceStatus(resource, ArmStackStatus.mapResourceStatus(status), e.getMessage());
            }
        } else {
            LOGGER.debug("Cloud resource status: {}", resourceStatus);
            armResourceStatus = new CloudResourceStatus(resource, ArmStackStatus.mapResourceStatus(status));
        }
        return armResourceStatus;
    }

    public String getResourceGroupName(CloudContext cloudContext) {
        return getStackName(cloudContext);
    }

    public boolean isExistingNetwork(Network network) {
        return isNoneEmpty(getCustomNetworkId(network)) && isNoneEmpty(getCustomResourceGroupName(network)) && isNoneEmpty(getCustomSubnetId(network));
    }

    public String getCustomNetworkId(Network network) {
        return network.getStringParameter(NETWORK_ID);
    }

    public String getCustomResourceGroupName(Network network) {
        return network.getStringParameter(RG_NAME);
    }

    public String getCustomSubnetId(Network network) {
        return network.getStringParameter(SUBNET_ID);
    }

    public void validateSubnetRules(AzureRMClient client, Network network) {
        if (isExistingNetwork(network)) {
            String resourceGroupName = getCustomResourceGroupName(network);
            String networkId = getCustomNetworkId(network);
            String subnetId = getCustomSubnetId(network);
            Map subnetProperties = client.getSubnetProperties(resourceGroupName, networkId, subnetId);
            Map networkSecurityGroup = (Map) subnetProperties.get("networkSecurityGroup");
            if (networkSecurityGroup != null) {
                validateSecurityGroup(client, networkSecurityGroup);
            }
        }
    }

    public void validateStorageType(CloudStack stack) {
        for (Group group : stack.getGroups()) {
            InstanceTemplate template = group.getInstances().get(0).getTemplate();
            String flavor = template.getFlavor();
            String volumeType = template.getVolumeType();
            ArmDiskType diskType = ArmDiskType.getByValue(volumeType);
            if (ArmDiskType.PREMIUM_LOCALLY_REDUNDANT.equals(diskType) && !flavor.contains("_DS")) {
                throw new CloudConnectorException("Only the DS instance types supports the premium storage.");
            }
        }
    }

    private void validateSecurityGroup(AzureRMClient client, Map networkSecurityGroup) {
        String securityGroupId = (String) networkSecurityGroup.get("id");
        String[] parts = securityGroupId.split("/");
        if (parts.length != ID_SEGMENTS) {
            LOGGER.info("Cannot get the security group's properties, id: {}", securityGroupId);
            return;
        }
        Map securityGroupProperties = client.getSecurityGroupProperties(parts[RG_PART], parts[SEC_GROUP_PART]);
        LOGGER.info("Retrieved security group properties: {}", securityGroupProperties);
        List securityRules = (List) securityGroupProperties.get("securityRules");
        boolean port22Found = false;
        boolean port443Found = false;
        for (Object securityRule : securityRules) {
            Map rule = (Map) securityRule;
            Map properties = (Map) rule.get("properties");
            if (isValidInboundRule(properties)) {
                String destinationPortRange = (String) properties.get("destinationPortRange");
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
        throw new CloudConnectorException("The specified subnet's security group does not allow traffic for port 22 and/or 443");
    }

    private boolean isValidInboundRule(Map properties) {
        String protocol = properties.get("protocol").toString().toLowerCase();
        return "inbound".equals(properties.get("direction").toString().toLowerCase())
                && ("tcp".equals(protocol) || "*".equals(protocol))
                && "allow".equals(properties.get("access").toString().toLowerCase());
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
