package com.sequenceiq.cloudbreak.cloud.azure.template;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.microsoft.azure.management.resources.Deployment;
import com.sequenceiq.cloudbreak.cloud.azure.AzureCloudResourceService;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;

@Component
public class AzureTransientDeploymentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureTransientDeploymentService.class);

    @Inject
    private AzureCloudResourceService azureCloudResourceService;

    public List<CloudResource> handleTransientDeployment(AzureClient client, String resourceGroupName, String deploymentName) {
        ResourceStatus deploymentStatus = client.getTemplateDeploymentStatus(resourceGroupName, deploymentName);
        if (deploymentStatus.isTransient()) {
            LOGGER.info("Template deployment {} has transient status {} , cancelling it now.", deploymentName, deploymentStatus);
            Deployment deployment = client.getTemplateDeployment(resourceGroupName, deploymentName);
            deployment.cancel();
            List<CloudResource> deployedResources = azureCloudResourceService.getDeploymentCloudResources(deployment);
            LOGGER.info("Found resources to be removed: {}", deployedResources);
            return deployedResources;
        }
        return Collections.emptyList();
    }
}
