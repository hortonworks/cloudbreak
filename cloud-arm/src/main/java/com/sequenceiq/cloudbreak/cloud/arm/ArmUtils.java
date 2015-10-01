package com.sequenceiq.cloudbreak.cloud.arm;

import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_ARM_CENTRAL_STORAGE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloud.azure.client.AzureRMClient;
import com.sequenceiq.cloudbreak.cloud.arm.status.ArmStackStatus;
import com.sequenceiq.cloudbreak.cloud.arm.view.ArmCredentialView;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

import groovyx.net.http.HttpResponseException;

@Component
public class ArmUtils {

    public static final int NOT_FOUND = 404;

    private static final Logger LOGGER = LoggerFactory.getLogger(ArmUtils.class);

    private static final int MAX_LENGTH_OF_RESOURCE_NAME = 24;

    @Value("${cb.arm.persistent.storage:" + CB_ARM_CENTRAL_STORAGE + "}")
    private String persistentStorage;

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
        return String.format("%s%s", cloudContext.getName(), cloudContext.getId());
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

    public boolean isPersistentStorage() {
        return !Strings.isNullOrEmpty(persistentStorage);
    }

    public String getImageResourceGroupName(CloudContext cloudContext) {
        if (isPersistentStorage()) {
            return persistentStorage;
        }
        return getResourceGroupName(cloudContext);
    }

    public String getResourceGroupName(CloudContext cloudContext) {
        return getStackName(cloudContext);
    }

    public String getStorageName(CloudCredential cloudCredential, CloudContext cloudContext, String region) {
        String result;
        if (isPersistentStorage()) {
            ArmCredentialView acv = new ArmCredentialView(cloudCredential);
            String subscriptionIdPart = acv.getSubscriptionId().replaceAll("-", "").toLowerCase();
            String regionInitials = WordUtils.initials(region, '_').toLowerCase();
            result = String.format("%s%s%s", persistentStorage, regionInitials, subscriptionIdPart).substring(0, MAX_LENGTH_OF_RESOURCE_NAME);
            LOGGER.info("Storage account name: {}", result);
        } else {
            result = cloudContext.getName().toLowerCase().replaceAll("\\s+|-", "") + cloudContext.getId() + cloudContext.getCreated();
        }
        if (result.length() > MAX_LENGTH_OF_RESOURCE_NAME) {
            return result.substring(result.length() - MAX_LENGTH_OF_RESOURCE_NAME, result.length());
        }
        return result;
    }

    public String getDiskContainerName(CloudContext cloudContext) {
        return getStackName(cloudContext);
    }


}
