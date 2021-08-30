package com.sequenceiq.cloudbreak.cloud.aws;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.services.cloudformation.model.AmazonCloudFormationException;
import com.amazonaws.services.cloudformation.model.GetTemplateRequest;
import com.amazonaws.services.cloudformation.model.GetTemplateResult;
import com.amazonaws.services.cloudformation.model.UpdateStackRequest;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.common.json.Json;

@Service
public class AwsLaunchTemplateImageUpdateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsLaunchTemplateImageUpdateService.class);

    @Inject
    private AwsCloudFormationClient awsClient;

    @Inject
    private AwsStackRequestHelper awsStackRequestHelper;

    public void updateImage(AuthenticatedContext authenticatedContext, CloudStack stack, CloudResource cfResource) {
        AwsCredentialView credentialView = new AwsCredentialView(authenticatedContext.getCloudCredential());
        String regionName = authenticatedContext.getCloudContext().getLocation().getRegion().getRegionName();
        AmazonCloudFormationClient cloudFormationClient = awsClient.createCloudFormationClient(credentialView, regionName);

        String cfStackName = cfResource.getName();
        String cfTemplate = getCfTemplate(cfResource, cloudFormationClient);
        Json templateJson = new Json(cfTemplate);

        String newCfTemplate = templateJson.getValue();
        UpdateStackRequest updateStackRequest = awsStackRequestHelper.createUpdateStackRequest(authenticatedContext, stack, cfStackName, newCfTemplate);
        try {
            cloudFormationClient.updateStack(updateStackRequest);
        } catch (AmazonCloudFormationException e) {
            if ("ValidationError".equals(e.getErrorCode()) && e.getErrorMessage().contains("No updates are to be performed")) {
                LOGGER.debug(String.format("CloudFormation is not updated as it is already in the latest state, name: %s", cfResource.getName()), e);
            } else {
                throw e;
            }
        }
    }

    private String getCfTemplate(CloudResource cfResource, AmazonCloudFormationClient cloudFormationClient) {
        String cfStackName = cfResource.getName();
        GetTemplateResult template = cloudFormationClient.getTemplate(new GetTemplateRequest().withStackName(cfStackName));
        return template.getTemplateBody();
    }
}
