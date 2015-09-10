package com.sequenceiq.cloudbreak.cloud.arm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloud.azure.client.AzureRMClient;
import com.sequenceiq.cloudbreak.cloud.arm.status.ArmStackStatus;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.domain.ResourceType;

import groovyx.net.http.HttpResponseException;

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

    public CloudResourceStatus templateStatus(CloudResource resource, Map<String, Object> templateDeployment, AzureRMClient access, String stackName) {
        String status = ((Map) templateDeployment.get("properties")).get("provisioningState").toString();
        LOGGER.info("Arm stack status of: {}  is: {}", resource.getName(), status);
        ResourceStatus resourceStatus = ArmStackStatus.mapResourceStatus(status);
        CloudResourceStatus armResourceStatus = null;
        if (ResourceStatus.FAILED.equals(resourceStatus)) {
            LOGGER.debug("Cloudresource status: {}", resourceStatus);
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
            LOGGER.debug("Cloudresource status: {}", resourceStatus);
            armResourceStatus = new CloudResourceStatus(resource, ArmStackStatus.mapResourceStatus(status));
        }
        return armResourceStatus;
    }

}
