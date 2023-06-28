package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.AwsUpdateService;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

@Service
public class AwsImageUpdateService {

    @Inject
    private AwsLaunchConfigurationUpdateService awsLaunchConfigurationUpdateService;

    @Inject
    private AwsLaunchTemplateUpdateService awsLaunchTemplateUpdateService;

    public void updateImage(AuthenticatedContext authenticatedContext, CloudStack stack, CloudResource cfResource) {
        String cfTemplate = stack.getTemplate();
        if (cfTemplate.contains(AwsUpdateService.LAUNCH_CONFIGURATION)) {
            awsLaunchConfigurationUpdateService.updateLaunchConfigurations(authenticatedContext, stack, cfResource,
                    Map.of(LaunchTemplateField.IMAGE_ID, stack.getImage().getImageName()), false);
        } else if (cfTemplate.contains(AwsUpdateService.LAUNCH_TEMPLATE)) {
            awsLaunchTemplateUpdateService.updateFieldsOnAllLaunchTemplate(authenticatedContext,
                    cfResource.getName(), Map.of(LaunchTemplateField.IMAGE_ID, stack.getImage().getImageName()), stack);
        } else {
            throw new NotImplementedException("Image update for stack template is not implemented yet.");
        }
    }
}
