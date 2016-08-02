package com.sequenceiq.cloudbreak.cloud.openstack.common;

import static com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants.NETWORK_ID;
import static com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants.ROUTER_ID;
import static com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants.SUBNET_ID;
import static org.apache.commons.lang3.StringUtils.isNoneEmpty;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.openstack4j.api.OSClient;
import org.openstack4j.model.heat.Stack;
import org.openstack4j.model.network.Subnet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Splitter;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.openstack.auth.OpenStackClient;
import com.sequenceiq.cloudbreak.cloud.openstack.status.HeatStackStatus;
import com.sequenceiq.cloudbreak.cloud.openstack.view.NeutronNetworkView;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

@Component
public class OpenStackUtils {

    public static final String CB_INSTANCE_GROUP_NAME = "cb_instance_group_name";
    public static final String CB_INSTANCE_PRIVATE_ID = "cb_instance_private_id";
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackUtils.class);

    @Value("${cb.max.openstack.resource.name.length:}")
    private int maxResourceNameLength;

    @Inject
    private OpenStackClient openStackClient;

    public CloudResource getHeatResource(List<CloudResource> resourceList) {
        for (CloudResource resource : resourceList) {
            if (resource.getType() == ResourceType.HEAT_STACK) {
                return resource;
            }
        }
        throw new CloudConnectorException(String.format("No resource found: %s", ResourceType.HEAT_STACK));
    }

    public String getPrivateInstanceId(String groupName, String privateId) {
        return getNormalizedGroupName(groupName) + "_"
                + privateId;
    }

    public String getPrivateInstanceId(Map<String, String> metadata) {
        return getPrivateInstanceId(metadata.get(CB_INSTANCE_GROUP_NAME), metadata.get(CB_INSTANCE_PRIVATE_ID));
    }

    public String getNormalizedGroupName(String groupName) {
        return groupName.replaceAll("_", "");
    }

    public String getStackName(AuthenticatedContext context) {
        return context.getCloudContext().getName() + "_" + context.getCloudContext().getId();
    }

    public CloudResourceStatus heatStatus(CloudResource resource, Stack heatStack) {
        String status = heatStack.getStatus();
        LOGGER.info("Heat stack status of: {}  is: {}", heatStack, status);
        CloudResourceStatus heatResourceStatus = new CloudResourceStatus(resource, HeatStackStatus.mapResourceStatus(status), heatStack.getStackStatusReason());
        LOGGER.debug("Cloud resource status: {}", heatResourceStatus);
        return heatResourceStatus;
    }

    public String adjustStackNameLength(String stackName) {
        return Splitter.fixedLength(maxResourceNameLength).splitToList(stackName).get(0);
    }

    public boolean isExistingNetwork(Network network) {
        return isNoneEmpty(getCustomNetworkId(network));
    }

    public boolean assignFloatingIp(Network network) {
        return new NeutronNetworkView(network).assignFloatingIp();
    }

    public String getCustomNetworkId(Network network) {
        return network.getStringParameter(NETWORK_ID);
    }

    public String getCustomRouterId(Network network) {
        return network.getStringParameter(ROUTER_ID);
    }

    public boolean isExistingSubnet(Network network) {
        return isNoneEmpty(getCustomSubnetId(network));
    }

    public String getCustomSubnetId(Network network) {
        return network.getStringParameter(SUBNET_ID);
    }

    public String getExistingSubnetCidr(AuthenticatedContext authenticatedContext, Network network) {
        if (isExistingSubnet(network)) {
            String subnetId = getCustomSubnetId(network);
            OSClient osClient = openStackClient.createOSClient(authenticatedContext);
            Subnet subnet = osClient.networking().subnet().get(subnetId);
            if (subnet == null) {
                throw new CloudConnectorException("The specified subnet does not exist: " + subnetId);
            }
            return subnet.getCidr();
        }
        return null;
    }

}
