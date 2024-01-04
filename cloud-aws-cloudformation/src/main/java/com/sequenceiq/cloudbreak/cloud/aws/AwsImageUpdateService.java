package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Inject;

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
                    Map.of(LaunchTemplateField.IMAGE_ID, stack.getImage().getImageName()));
        } else if (cfTemplate.contains(AwsUpdateService.LAUNCH_TEMPLATE)) {
            Map<LaunchTemplateField, String> updatableFields = new HashMap<>();
            updatableFields.put(LaunchTemplateField.IMAGE_ID, stack.getImage().getImageName());
            updatableFields.put(LaunchTemplateField.ROOT_DISK_PATH, "");
            awsLaunchTemplateUpdateService.updateFieldsOnAllLaunchTemplate(authenticatedContext,
                    cfResource.getName(), updatableFields, stack);
        } else {
            throw new NotImplementedException("Image update for stack template is not implemented yet.");
        }
    }
}
