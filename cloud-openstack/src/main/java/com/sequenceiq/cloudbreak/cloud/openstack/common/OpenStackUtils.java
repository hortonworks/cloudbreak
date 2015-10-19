package com.sequenceiq.cloudbreak.cloud.openstack.common;

import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_MAX_OPENSTACK_RESOURCE_NAME_LENGTH;

import java.util.List;
import java.util.Map;

import org.openstack4j.model.heat.Stack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Splitter;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.openstack.status.HeatStackStatus;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

@Component
public class OpenStackUtils {

    public static final String CB_INSTANCE_GROUP_NAME = "cb_instance_group_name";
    public static final String CB_INSTANCE_PRIVATE_ID = "cb_instance_private_id";
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackUtils.class);

    @Value("${cb.max.openstack.resource.name.length:" + CB_MAX_OPENSTACK_RESOURCE_NAME_LENGTH + "}")
    private int maxResourceNameLength;


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

    public CloudResourceStatus heatStatus(CloudResource resource, Stack heatStack) {
        String status = heatStack.getStatus();
        LOGGER.info("Heat stack status of: {}  is: {}", heatStack, status);
        CloudResourceStatus heatResourceStatus = new CloudResourceStatus(resource, HeatStackStatus.mapResourceStatus(status), heatStack.getStackStatusReason());
        LOGGER.debug("Cloudresource status: {}", heatResourceStatus);
        return heatResourceStatus;
    }

    public String adjustStackNameLength(String stackName) {
        return new String(Splitter.fixedLength(maxResourceNameLength).splitToList(stackName).get(0));
    }

}
