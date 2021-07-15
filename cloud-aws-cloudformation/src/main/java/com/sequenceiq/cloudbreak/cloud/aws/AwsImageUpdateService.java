package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

@Service
public class AwsImageUpdateService {

    @Inject
    private AwsLaunchConfigurationImageUpdateService awsLaunchConfigurationImageUpdateService;

    @Inject
    private AwsLaunchTemplateUpdateService awsLaunchTemplateUpdateService;

    public void updateImage(AuthenticatedContext authenticatedContext, CloudStack stack, CloudResource cfResource) {
        String cfTemplate = stack.getTemplate();
        if (cfTemplate.contains("AWS::AutoScaling::LaunchConfiguration")) {
            awsLaunchConfigurationImageUpdateService.updateImage(authenticatedContext, stack, cfResource);
        } else if (cfTemplate.contains("AWS::EC2::LaunchTemplate")) {
            awsLaunchTemplateUpdateService.updateFields(authenticatedContext,
                    cfResource, Map.of(LaunchTemplateField.IMAGE_ID, stack.getImage().getImageId()));
        } else {
            throw new NotImplementedException("Image update for stack template is not implemented yet.");
        }
    }
}
