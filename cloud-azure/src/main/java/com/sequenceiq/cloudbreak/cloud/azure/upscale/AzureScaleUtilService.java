package com.sequenceiq.cloudbreak.cloud.azure.upscale;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.microsoft.azure.CloudError;
import com.microsoft.azure.CloudException;
import com.microsoft.azure.management.resources.Deployment;
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

    public void checkIfQuotaLimitIssued(CloudException e) throws QuotaExceededException {
        if (e.body() != null && e.body().details() != null) {
            List<CloudError> errorDetails = e.body().details();
            for (CloudError errorDetail : errorDetails) {
                if ("QuotaExceeded".equals(errorDetail.code())) {
                    Pattern pattern = Pattern.compile(".*Current Limit: ([0-9]+), Current Usage: ([0-9]+), Additional Required: ([0-9]+).*");
                    Matcher matcher = pattern.matcher(errorDetail.message());
                    if (matcher.find()) {
                        int currentLimit = Integer.parseInt(matcher.group(CURRENT_LIMIT_GROUP));
                        int currentUsage = Integer.parseInt(matcher.group(CURRENT_USAGE_GROUP));
                        int additionalRequired = Integer.parseInt(matcher.group(ADDITIONAL_REQUIRED_GROUP));
                        throw new QuotaExceededException(currentLimit, currentUsage, additionalRequired, errorDetail.message(), e);
                    } else {
                        LOGGER.warn("Quota exceeded pattern does not match: {}", errorDetail.message());
                    }
                }
            }
        }
    }

    public void rollbackResources(AuthenticatedContext ac, AzureClient client, CloudStack stack, CloudContext cloudContext,
        List<CloudResource> resources, DateTime preDeploymentTime) {
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
        List<CloudResource> templateResources = new ArrayList<>();
        List<CloudResource> newInstances = new ArrayList<>();
        List<CloudResource> osDiskResources = new ArrayList<>();

        templateResources.addAll(azureCloudResourceService.getDeploymentCloudResources(templateDeployment));
        List<Group> scaledGroups = cloudResourceHelper.getScaledGroups(stack);
        newInstances.addAll(azureCloudResourceService.getInstanceCloudResources(stackName, templateResources, scaledGroups, resourceGroupName));
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

    public AzureCloudResourceService azureCloudResourceService() {
        return azureCloudResourceService;
    }

    public CloudResource getArmTemplate(List<CloudResource> resources, String stackName) {
        return resources.stream().filter(r -> r.getType() == ResourceType.ARM_TEMPLATE).findFirst()
                .orElseThrow(() -> new CloudConnectorException(String.format("Arm Template not found for: %s  ", stackName)));
    }

    private boolean isTemplateDeploymentObsolete(DateTime preDeploymentTime, Deployment templateDeployment) {
        DateTime deploymentTimestamp = templateDeployment.timestamp();
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
