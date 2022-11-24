package com.sequenceiq.cloudbreak.cloud.azure.upscale;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.microsoft.azure.CloudException;
import com.microsoft.azure.management.resources.Deployment;
import com.sequenceiq.cloudbreak.cloud.azure.AzureInstanceTemplateOperation;
import com.sequenceiq.cloudbreak.cloud.azure.AzureUtils;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.template.AzureTemplateDeploymentService;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureStackView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.exception.QuotaExceededException;
import com.sequenceiq.cloudbreak.cloud.exception.RolledbackResourcesException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.service.Retry;

@Component
public class AzureVerticalScaleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureVerticalScaleService.class);

    @Inject
    private AzureUtils azureUtils;

    @Inject
    private AzureScaleUtilService azureScaleUtilService;

    @Inject
    private AzureTemplateDeploymentService azureTemplateDeploymentService;

    public List<CloudResourceStatus> verticalScale(AuthenticatedContext ac, CloudStack stack, List<CloudResource> resources, AzureStackView azureStackView,
            AzureClient client) throws QuotaExceededException {
        CloudContext cloudContext = ac.getCloudContext();
        String stackName = azureUtils.getStackName(cloudContext);
        try {
            CloudResource armTemplate = azureScaleUtilService.getArmTemplate(resources, stackName);

            Deployment templateDeployment =
                    azureTemplateDeploymentService.getTemplateDeployment(client, stack, ac, azureStackView, AzureInstanceTemplateOperation.VERTICAL_SCALE);
            LOGGER.info("Created template deployment for upscale: {}", templateDeployment.exportTemplate().template());

            return List.of(new CloudResourceStatus(armTemplate, ResourceStatus.IN_PROGRESS));
        } catch (Retry.ActionFailedException e) {
            LOGGER.error("Retry.ActionFailedException happened", e);
            throw azureUtils.convertToCloudConnectorException(e.getCause(), "Stack upscale");
        } catch (CloudException e) {
            LOGGER.error("CloudException happened", e);
            azureScaleUtilService.checkIfQuotaLimitIssued(e);
            throw azureUtils.convertToCloudConnectorException(e, "Stack upscale");
        } catch (RolledbackResourcesException e) {
            LOGGER.error("RolledbackResourcesException happened", e);
            throw new CloudConnectorException(String.format("Could not upscale Azure infrastructure, infrastructure was rolled back with resources: %s, %s",
                    stackName, e.getMessage()), e);
        } catch (Exception e) {
            LOGGER.error("Exception happened", e);
            throw new CloudConnectorException(String.format("Could not upscale Azure infrastructure, infrastructure was rolled back: %s, %s", stackName,
                    e.getMessage()), e);
        }
    }

}
