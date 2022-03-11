package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.aws.AwsImageUpdateService;
import com.sequenceiq.cloudbreak.cloud.aws.AwsLaunchTemplateUpdateService;
import com.sequenceiq.cloudbreak.cloud.aws.LaunchTemplateField;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.common.api.type.CommonResourceType;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class AwsUpdateService {
    public static final String LAUNCH_CONFIGURATION = "AWS::AutoScaling::LaunchConfiguration";

    public static final String LAUNCH_TEMPLATE = "AWS::EC2::LaunchTemplate";

    @Inject
    private AwsImageUpdateService awsImageUpdateService;

    @Inject
    private AwsLaunchTemplateUpdateService awsLaunchTemplateUpdateService;

    public List<CloudResourceStatus> update(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources) {
        ArrayList<CloudResourceStatus> cloudResourceStatuses = new ArrayList<>();
        if (!resources.isEmpty() && resources.stream().anyMatch(resource -> CommonResourceType.TEMPLATE == resource.getType().getCommonResourceType()
                && StringUtils.isNotBlank(resource.getStringParameter(CloudResource.IMAGE)))) {

            List<CloudResource> launchConfigurationResources = resources.stream()
                    .filter(resource -> CommonResourceType.TEMPLATE == resource.getType().getCommonResourceType()
                            && StringUtils.isNotBlank(resource.getStringParameter(CloudResource.IMAGE))).collect(Collectors.toList());

            CloudResource cfResource = getCloudFormationStack(resources);
            awsImageUpdateService.updateImage(authenticatedContext, stack, cfResource);

            launchConfigurationResources.forEach(cloudResource -> cloudResourceStatuses.add(new CloudResourceStatus(cloudResource, ResourceStatus.UPDATED)));
        }
        return cloudResourceStatuses;
    }

    public void updateUserData(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources, String userData) {
        String cfTemplate = stack.getTemplate();
        if (cfTemplate.contains(LAUNCH_TEMPLATE)) {
            CloudResource cfResource = getCloudFormationStack(resources);
            awsLaunchTemplateUpdateService.updateFields(authenticatedContext, cfResource.getName(), Map.of(LaunchTemplateField.USER_DATA,
                    Base64.getEncoder().encodeToString(userData.getBytes())));
        } else {
            throw new NotImplementedException("UserData update for stack template is not implemented yet, only for AWS::EC2::LaunchTemplate.");
        }
    }

    public void checkUpdate(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources) throws Exception {
        CloudResource cfResource = getCloudFormationStack(resources);
        awsLaunchTemplateUpdateService.updateFields(authenticatedContext, cfResource.getName(), Map.of(LaunchTemplateField.DESCRIPTION,
                String.format("Latest modifyLaunchTemplate check for upgrade: %s", new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(new Date()))),
                true);
    }

    private CloudResource getCloudFormationStack(List<CloudResource> resources) {
        return resources.stream().filter(resource -> ResourceType.CLOUDFORMATION_STACK == resource.getType()).findFirst()
                .orElseThrow(() -> new NotFoundException("CloudFormation stack is not found, the resource might have been deleted."));
    }
}
