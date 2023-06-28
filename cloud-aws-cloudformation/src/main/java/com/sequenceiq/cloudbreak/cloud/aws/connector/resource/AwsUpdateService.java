package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.swing.text.html.Option;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.UpdateType;
import com.sequenceiq.cloudbreak.cloud.aws.AwsImageUpdateService;
import com.sequenceiq.cloudbreak.cloud.aws.AwsLaunchConfigurationUpdateService;
import com.sequenceiq.cloudbreak.cloud.aws.AwsLaunchTemplateUpdateService;
import com.sequenceiq.cloudbreak.cloud.aws.LaunchTemplateField;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatusWithMessage;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.common.api.type.CommonResourceType;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class AwsUpdateService {
    public static final String LAUNCH_CONFIGURATION = "AWS::AutoScaling::LaunchConfiguration";

    public static final String LAUNCH_TEMPLATE = "AWS::EC2::LaunchTemplate";

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsUpdateService.class);

    @Inject
    private AwsImageUpdateService awsImageUpdateService;

    @Inject
    private AwsLaunchTemplateUpdateService awsLaunchTemplateUpdateService;

    @Inject
    private AwsLaunchConfigurationUpdateService launchConfigurationUpdateService;

    public CloudResourceStatusWithMessage update(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources,
        UpdateType type, Optional<String> groupName) {
        CloudResourceStatusWithMessage message = new CloudResourceStatusWithMessage.Builder().build();
        ArrayList<CloudResourceStatus> cloudResourceStatuses = new ArrayList<>();
        LOGGER.info("The update method which will be followed is {}.", type);
        if (!resources.isEmpty()) {
            if (type.equals(UpdateType.IMAGE_UPDATE)) {
                if (resources.stream().anyMatch(resource -> CommonResourceType.TEMPLATE == resource.getType().getCommonResourceType()
                        && StringUtils.isNotBlank(resource.getStringParameter(CloudResource.IMAGE)))) {
                    List<CloudResource> launchConfigurationResources = resources.stream()
                            .filter(resource -> CommonResourceType.TEMPLATE == resource.getType().getCommonResourceType()
                                    && StringUtils.isNotBlank(resource.getStringParameter(CloudResource.IMAGE))).collect(Collectors.toList());

                    CloudResource cfResource = getCloudFormationStack(resources);
                    awsImageUpdateService.updateImage(authenticatedContext, stack, cfResource);

                    launchConfigurationResources.forEach(cloudResource ->
                            cloudResourceStatuses.add(new CloudResourceStatus(cloudResource, ResourceStatus.UPDATED)));
                }
            } else if (type.equals(UpdateType.VERTICAL_SCALE)) {
                message = updateWithVerticalScaling(authenticatedContext, stack, resources, groupName);
            }
        }
        return message;
    }

    private CloudResourceStatusWithMessage updateWithVerticalScaling(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources,
        Optional<String> groupName) {
        String cfTemplate = stack.getTemplate();
        CloudResource cfResource = getCloudFormationStack(resources);
        CloudResourceStatusWithMessage message = new CloudResourceStatusWithMessage.Builder().build();
        for (Group group : stack.getGroups()) {
            boolean onlyMetadataCheckRequired = false;
            if (group.getName().equalsIgnoreCase(groupName.get())) {
                onlyMetadataCheckRequired = true;
            }
            Map<LaunchTemplateField, String> updatableFields = Map.of(
                    LaunchTemplateField.INSTANCE_TYPE,
                    group.getReferenceInstanceConfiguration().getTemplate().getFlavor(),
                    LaunchTemplateField.ROOT_DISK,
                    String.valueOf(group.getRootVolumeSize())
            );
            if (cfTemplate.contains(LAUNCH_TEMPLATE)) {
                LOGGER.info("Update fields on launchtemplate {} on group {} on cf {}", updatableFields, group.getName(), cfResource.getName());
                message = awsLaunchTemplateUpdateService.updateLaunchTemplate(updatableFields, authenticatedContext, cfResource.getName(), group, stack, onlyMetadataCheckRequired);
            } else {
                LOGGER.info("Update fields on launchconfiguration {} on group {} on cf {}", updatableFields, group.getName(), cfResource.getName());
                message = launchConfigurationUpdateService.updateLaunchConfigurations(authenticatedContext, stack, cfResource, updatableFields, group, onlyMetadataCheckRequired);
            }
        }
        return message;
    }

    public void updateUserData(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources,
            Map<InstanceGroupType, String> userData) {
        String cfTemplate = stack.getTemplate();
        if (cfTemplate.contains(LAUNCH_TEMPLATE)) {
            CloudResource cfResource = getCloudFormationStack(resources);
            stack.getGroups().forEach(group -> {
                String groupUserData = userData.get(group.getType());
                String encodedGroupUserData = Base64.getEncoder().encodeToString(groupUserData.getBytes());
                Map<LaunchTemplateField, String> updatableFields = Map.of(LaunchTemplateField.USER_DATA, encodedGroupUserData);
                awsLaunchTemplateUpdateService.updateLaunchTemplate(updatableFields, authenticatedContext, cfResource.getName(),
                        group, stack, false);
            });
        } else {
            throw new NotImplementedException("UserData update for stack template is not implemented yet, only for AWS::EC2::LaunchTemplate.");
        }
    }

    public void checkUpdate(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources) throws Exception {
        CloudResource cfResource = getCloudFormationStack(resources);
        awsLaunchTemplateUpdateService.updateFieldsOnAllLaunchTemplate(authenticatedContext, cfResource.getName(), Map.of(LaunchTemplateField.DESCRIPTION,
                String.format("Latest modifyLaunchTemplate check for upgrade: %s",
                        new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(new Date()))), true, stack);
    }

    private CloudResource getCloudFormationStack(List<CloudResource> resources) {
        return resources.stream().filter(resource -> ResourceType.CLOUDFORMATION_STACK == resource.getType()).findFirst()
                .orElseThrow(() -> new NotFoundException("CloudFormation stack is not found, the resource might have been deleted."));
    }
}
