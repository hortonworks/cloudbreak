package com.sequenceiq.cloudbreak.cloud.azure.util;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.azure.resourcemanager.resources.models.Deployment;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.status.AzureStatusMapper;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;

@Component
public class AzureTemplateDeploymentFailureReasonProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureTemplateDeploymentFailureReasonProvider.class);

    public Optional<String> getFailureMessage(String resourceGroupName, String deploymentName, AzureClient client) {
        try {
            Deployment deployment = client.getTemplateDeployment(resourceGroupName, deploymentName);
            LOGGER.debug("The status of the {} deployment is: {}", deploymentName, deployment.provisioningState());
            if (!ResourceStatus.FAILED.equals(AzureStatusMapper.mapResourceStatus(deployment.provisioningState()))) {
                return Optional.empty();
            }
            return client.getTemplateDeploymentOperations(resourceGroupName, deploymentName).getAll()
                    .stream()
                    .filter(operation -> "Failed".equals(operation.provisioningState()))
                    .peek(operation -> LOGGER.debug("Failed deployment operation found: {}", operation))
                    .findFirst()
                    .map(operation -> (String) operation.statusMessage());
        } catch (Exception e) {
            LOGGER.debug("Failed to get operation failure message.", e);
            return Optional.empty();
        }
    }
}


