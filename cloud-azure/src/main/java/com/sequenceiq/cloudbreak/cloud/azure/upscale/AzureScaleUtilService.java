package com.sequenceiq.cloudbreak.cloud.azure.upscale;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.azure.core.management.exception.ManagementError;
import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.resources.models.Deployment;
import com.sequenceiq.cloudbreak.cloud.azure.AzureCloudResourceService;
import com.sequenceiq.cloudbreak.cloud.azure.AzureResourceGroupMetadataProvider;
import com.sequenceiq.cloudbreak.cloud.azure.AzureTerminationHelperService;
import com.sequenceiq.cloudbreak.cloud.azure.AzureUtils;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.exception.QuotaExceededException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.transform.CloudResourceHelper;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class AzureScaleUtilService {

    private static final int ADDITIONAL_REQUIRED_GROUP = 3;

    private static final int CURRENT_USAGE_GROUP = 2;

    private static final int CURRENT_LIMIT_GROUP = 1;

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureScaleUtilService.class);

    @Inject
    private AzureUtils azureUtils;

    @Inject
    private CloudResourceHelper cloudResourceHelper;

    @Inject
    private AzureCloudResourceService azureCloudResourceService;

    @Inject
    private AzureResourceGroupMetadataProvider azureResourceGroupMetadataProvider;

    @Inject
    private AzureTerminationHelperService azureTerminationHelperService;

    public void checkIfQuotaLimitIssued(ManagementException e) throws QuotaExceededException {
        if (e.getValue() != null && e.getValue().getDetails() != null) {
            List<? extends ManagementError> errorDetails = e.getValue().getDetails();
            for (ManagementError errorDetail : errorDetails) {
                if ("QuotaExceeded".equals(errorDetail.getCode())) {
                    Pattern pattern = Pattern.compile(".*Current Limit: ([0-9]+), Current Usage: ([0-9]+), Additional Required: ([0-9]+).*");
                    Matcher matcher = pattern.matcher(errorDetail.getMessage());
                    if (matcher.find()) {
                        int currentLimit = Integer.parseInt(matcher.group(CURRENT_LIMIT_GROUP));
                        int currentUsage = Integer.parseInt(matcher.group(CURRENT_USAGE_GROUP));
                        int additionalRequired = Integer.parseInt(matcher.group(ADDITIONAL_REQUIRED_GROUP));
                        throw new QuotaExceededException(currentLimit, currentUsage, additionalRequired, errorDetail.getMessage(), e);
                    } else {
                        LOGGER.warn("Quota exceeded pattern does not match: {}", errorDetail.getMessage());
                    }
                }
            }
        }
    }

    public void rollbackResources(AuthenticatedContext ac, AzureClient client, CloudStack stack, CloudContext cloudContext,
            List<CloudResource> resources, OffsetDateTime preDeploymentTime) {
        String stackName = azureUtils.getStackName(cloudContext);
        String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, stack);

        Deployment templateDeployment = client.getTemplateDeployment(resourceGroupName, stackName);
        if (templateDeployment == null) {
            LOGGER.info("TemplateDeployment with resourceGroupName {} and deploymentName {} not found. Rollback cancelled.", resourceGroupName, stackName);
            return;
        }
        if (isTemplateDeploymentObsolete(preDeploymentTime, templateDeployment)) {
            LOGGER.info("TemplateDeployment with resourceGroupName {} and deploymentName {} is obsolete. Rollback cancelled.", resourceGroupName, stackName);
            return;
        }
        List<CloudResource> osDiskResources = new ArrayList<>();

        List<CloudResource> templateResources = new ArrayList<>(azureCloudResourceService.getDeploymentCloudResources(templateDeployment));
        List<Group> scaledGroups = cloudResourceHelper.getScaledGroups(stack);
        List<CloudResource> newInstances = new ArrayList<>(azureCloudResourceService.getInstanceCloudResources(
                stackName, templateResources, scaledGroups, resourceGroupName));
        if (!newInstances.isEmpty()) {
            osDiskResources.addAll(azureCloudResourceService.getAttachedOsDiskResources(newInstances, resourceGroupName, client));
        } else {
            LOGGER.warn("Skipping OS disk collection as there was no VM instance found amongst cloud resources for {}!", stackName);
        }

        List<CloudInstance> newCloudInstances = getNewInstances(newInstances);
        List<CloudResource> allRemovableResource = new ArrayList<>();
        allRemovableResource.addAll(templateResources);
        allRemovableResource.addAll(osDiskResources);

        azureTerminationHelperService.downscale(ac, stack, newCloudInstances, resources, allRemovableResource);
    }

    public CloudResource getArmTemplate(List<CloudResource> resources, String stackName) {
        return resources.stream().filter(r -> r.getType() == ResourceType.ARM_TEMPLATE).findFirst()
                .orElseThrow(() -> new CloudConnectorException(String.format("Arm Template not found for: %s  ", stackName)));
    }

    private boolean isTemplateDeploymentObsolete(OffsetDateTime preDeploymentTime, Deployment templateDeployment) {
        OffsetDateTime deploymentTimestamp = templateDeployment.timestamp();
        return deploymentTimestamp == null || deploymentTimestamp.isBefore(preDeploymentTime);
    }

    private List<CloudInstance> getNewInstances(List<CloudResource> newInstances) {
        List<CloudInstance> newCloudInstances = newInstances.stream()
                .map(cloudResource -> new CloudInstance(
                        cloudResource.getInstanceId(),
                        null,
                        null,
                        null,
                        null,
                        cloudResource.getParameters()))
                .collect(Collectors.toList());
        LOGGER.debug("Created instances to be removed {}", newCloudInstances.toString());
        return newCloudInstances;
    }

}
