package com.sequenceiq.cloudbreak.cloud.azure.util;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.azure.resourcemanager.resources.models.Deployment;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.status.AzureStatusMapper;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

@Component
public class AzureTemplateDeploymentFailureReasonProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureTemplateDeploymentFailureReasonProvider.class);

    public Optional<String> getFailureMessage(String resourceGroupName, String deploymentName, AzureClient client) {
        try {
            Optional<Deployment> deployment = client.getTemplateDeployment(resourceGroupName, deploymentName);
            if (deployment.isEmpty()) {
                throw new CloudbreakServiceException("Could not fetch template deployment using resource group name and deployment name");
            }
            LOGGER.debug("The status of the {} deployment is: {}", deploymentName, deployment.get().provisioningState());
            if (!ResourceStatus.FAILED.equals(AzureStatusMapper.mapResourceStatus(deployment.get().provisioningState()))) {
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


