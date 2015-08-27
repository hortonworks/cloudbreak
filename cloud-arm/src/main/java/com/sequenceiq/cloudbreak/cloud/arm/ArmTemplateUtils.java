package com.sequenceiq.cloudbreak.cloud.arm;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.arm.status.ArmStackStatus;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.domain.ResourceType;

@Component
public class ArmTemplateUtils {

    public static final int NOT_FOUND = 404;

    private static final Logger LOGGER = LoggerFactory.getLogger(ArmTemplateUtils.class);

    public CloudResource getTemplateResource(List<CloudResource> resourceList) {
        for (CloudResource resource : resourceList) {
            if (resource.getType() == ResourceType.ARM_TEMPLATE) {
                return resource;
            }
        }
        throw new CloudConnectorException(String.format("No resource found: %s", ResourceType.ARM_TEMPLATE));
    }

    public String getPrivateInstanceId(String stackName, String groupName, String privateId) {
        return String.format("%s%s%s", stackName, groupName.replaceAll("_", ""), privateId);
    }

    public String getStackName(CloudContext cloudContext) {
        return String.format("%s%s", cloudContext.getStackName(), cloudContext.getStackId());
    }

    public String getLoadBalancerId(String stackName) {
        return String.format("%s%s", stackName, "lb");
    }

    public CloudResourceStatus templateStatus(CloudResource resource, Map<String, Object> templateDeployment) {
        String status = ((Map) templateDeployment.get("properties")).get("provisioningState").toString();
        LOGGER.info("Arm stack status of: {}  is: {}", resource.getReference(), status);
        CloudResourceStatus armResourceStatus = new CloudResourceStatus(resource, ArmStackStatus.mapResourceStatus(status));
        LOGGER.debug("Cloudresource status: {}", armResourceStatus);
        return armResourceStatus;
    }

}
