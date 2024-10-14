package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.UpdateType;
import com.sequenceiq.cloudbreak.cloud.aws.AutoScalingGroupHandler;
import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.AwsImageUpdateService;
import com.sequenceiq.cloudbreak.cloud.aws.AwsLaunchConfigurationUpdateService;
import com.sequenceiq.cloudbreak.cloud.aws.AwsLaunchTemplateUpdateService;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil;
import com.sequenceiq.cloudbreak.cloud.aws.LaunchTemplateField;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsImdsUtil;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.common.base64.Base64Util;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.common.api.type.CommonResourceType;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.common.model.AwsDiskType;

import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup;
import software.amazon.awssdk.services.ec2.model.HttpTokensState;
import software.amazon.awssdk.services.ec2.model.LaunchTemplateBlockDeviceMapping;
import software.amazon.awssdk.services.ec2.model.LaunchTemplateEbsBlockDevice;

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

    @Inject
    private AutoScalingGroupHandler autoScalingGroupHandler;

    @Inject
    private AwsCloudFormationClient awsClient;

    @Inject
    private CloudFormationStackUtil cfStackUtil;

    public List<CloudResourceStatus> update(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources,
            UpdateType type, Optional<String> targetGroupName) {
        ArrayList<CloudResourceStatus> cloudResourceStatuses = new ArrayList<>();
        LOGGER.info("The update method which will be followed is {}.", type);
        if (!resources.isEmpty()) {
            if (type.equals(UpdateType.IMAGE_UPDATE)) {
                if (resources.stream().anyMatch(resource -> CommonResourceType.TEMPLATE == resource.getType().getCommonResourceType()
                        && StringUtils.isNotBlank(resource.getStringParameter(CloudResource.IMAGE)))) {
                    List<CloudResource> launchConfigurationResources = resources.stream()
                            .filter(resource -> CommonResourceType.TEMPLATE == resource.getType().getCommonResourceType()
                                    && StringUtils.isNotBlank(resource.getStringParameter(CloudResource.IMAGE))).toList();

                    CloudResource cfResource = getCloudFormationStack(resources);
                    awsImageUpdateService.updateImage(authenticatedContext, stack, cfResource);

                    launchConfigurationResources.forEach(cloudResource ->
                            cloudResourceStatuses.add(new CloudResourceStatus(cloudResource, ResourceStatus.UPDATED)));
                }
            } else if (type.equals(UpdateType.VERTICAL_SCALE)) {
                updateWithVerticalScaling(authenticatedContext, stack, resources, targetGroupName, true);
            } else if (type.equals(UpdateType.VERTICAL_SCALE_WITHOUT_INSTANCES)) {
                updateWithVerticalScaling(authenticatedContext, stack, resources, targetGroupName, false);
            } else if (AwsImdsUtil.APPLICABLE_UPDATE_TYPES.contains(type)) {
                updateInstanceMetadataOptions(authenticatedContext, stack, resources, type);
            }
        } else if (type.equals(UpdateType.PROVIDER_TEMPLATE_UPDATE)) {
            updateLauchTemplateOfAutoscalingGroup(authenticatedContext, stack);
        }
        return cloudResourceStatuses;
    }

    private void updateInstanceMetadataOptions(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources,
            UpdateType updateType) {
        AwsImdsUtil.validateInstanceMetadataUpdate(updateType, stack);
        HttpTokensState httpTokensState = AwsImdsUtil.getHttpTokensStateByUpdateType(updateType);
        String cfTemplate = stack.getTemplate();
        CloudResource cfResource = getCloudFormationStack(resources);
        for (Group group : stack.getGroups()) {
            Map<LaunchTemplateField, String> updatableFields = Map.of(LaunchTemplateField.HTTP_METADATA_OPTIONS, httpTokensState.toString());
            if (cfTemplate.contains(LAUNCH_TEMPLATE)) {
                LOGGER.info("Update fields on launchtemplate {} on group {} on cf {}", updatableFields, group.getName(), cfResource.getName());
                awsLaunchTemplateUpdateService.updateLaunchTemplate(updatableFields, authenticatedContext, cfResource.getName(), group, stack, true);
            } else {
                LOGGER.info("Update fields on launchconfiguration {} on group {} on cf {}", updatableFields, group.getName(), cfResource.getName());
                launchConfigurationUpdateService.updateLaunchConfigurations(authenticatedContext, stack, cfResource, updatableFields, group, true);
            }
        }
    }

    private void updateWithVerticalScaling(AuthenticatedContext authenticatedContext, CloudStack stack,
            List<CloudResource> resources, Optional<String> groupName, boolean updateInstances) {
        String cfTemplate = stack.getTemplate();
        CloudResource cfResource = getCloudFormationStack(resources);
        for (Group group : stack.getGroups()) {
            if (!groupName.isEmpty() && group.getName().equalsIgnoreCase(groupName.get())) {
                Map<LaunchTemplateField, String> updatableFields = Map.of(
                        LaunchTemplateField.INSTANCE_TYPE,
                        group.getReferenceInstanceTemplate().getFlavor(),
                        LaunchTemplateField.ROOT_DISK_SIZE,
                        String.valueOf(group.getRootVolumeSize())
                );
                if (cfTemplate.contains(LAUNCH_TEMPLATE)) {
                    LOGGER.info("Update fields on launchtemplate {} on group {} on cf {}", updatableFields, group.getName(), cfResource.getName());
                    awsLaunchTemplateUpdateService.updateLaunchTemplate(updatableFields, authenticatedContext, cfResource.getName(), group, stack,
                            updateInstances);
                } else {
                    LOGGER.info("Update fields on launchconfiguration {} on group {} on cf {}", updatableFields, group.getName(), cfResource.getName());
                    launchConfigurationUpdateService.updateLaunchConfigurations(authenticatedContext, stack, cfResource, updatableFields, group,
                            updateInstances);
                }
            }
        }
    }

    public void updateUserData(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources,
            Map<InstanceGroupType, String> userData) {
        String cfTemplate = stack.getTemplate();
        if (cfTemplate.contains(LAUNCH_TEMPLATE)) {
            CloudResource cfResource = getCloudFormationStack(resources);
            stack.getGroups().forEach(group -> {
                String groupUserData = userData.get(group.getType());
                if (StringUtils.isNotEmpty(groupUserData)) {
                    String encodedGroupUserData = Base64Util.encode(groupUserData);
                    Map<LaunchTemplateField, String> updatableFields = Map.of(LaunchTemplateField.USER_DATA, encodedGroupUserData);
                    awsLaunchTemplateUpdateService.updateLaunchTemplate(updatableFields, authenticatedContext, cfResource.getName(), group, stack,
                            Boolean.FALSE);
                } else {
                    String msg = String.format("The user data is empty for group '%s' and group type '%s' which should not happen!",
                            group.getName(), group.getType());
                    LOGGER.warn(msg);
                    throw new IllegalStateException(msg);
                }
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

    private void updateLauchTemplateOfAutoscalingGroup(AuthenticatedContext ac, CloudStack stack) {
        String regionName = ac.getCloudContext().getLocation().getRegion().value();
        AwsCredentialView credentialView = new AwsCredentialView(ac.getCloudCredential());
        AmazonCloudFormationClient cloudFormationClient = awsClient.createCloudFormationClient(credentialView, regionName);
        AmazonAutoScalingClient autoScalingClient = awsClient.createAutoScalingClient(credentialView, regionName);
        AmazonEc2Client ec2Client = awsClient.createEc2Client(credentialView, regionName);
        Map<String, Group> groupByName = stack.getGroups().stream().collect(Collectors.toMap(Group::getName, group -> group));
        Map<String, AutoScalingGroup> autoScalingGroupMap = autoScalingGroupHandler
                .autoScalingGroupByName(cloudFormationClient, autoScalingClient, cfStackUtil.getCfStackName(ac));
        for (String groupName : groupByName.keySet()) {
            Group group = groupByName.get(groupName);
            Map<LaunchTemplateField, String> updatableFields = Map.of(LaunchTemplateField.ROOT_DISK_SIZE, String.valueOf(group.getRootVolumeSize()),
                    LaunchTemplateField.ROOT_VOLUME_TYPE, group.getRootVolumeType() != null ? group.getRootVolumeType().toLowerCase(Locale.ROOT)
                            : AwsDiskType.Gp3.value());
            Optional<AutoScalingGroup> autoScalingGroupOptional = autoScalingGroupMap.keySet().stream().filter(key -> key.contains(groupName))
                    .map(autoScalingGroupMap::get).findFirst();
            if (autoScalingGroupOptional.isPresent()) {
                AutoScalingGroup autoScalingGroup = autoScalingGroupOptional.get();
                List<LaunchTemplateBlockDeviceMapping> currentBlockDeviceMappings =
                        awsLaunchTemplateUpdateService.getBlockDeviceMappingFromAutoScalingGroup(ac, autoScalingGroup);
                boolean updateLaunchTemplate = shouldUpdateLaunchTemplate(currentBlockDeviceMappings, group);
                if (updateLaunchTemplate) {
                    awsLaunchTemplateUpdateService.updateLaunchTemplate(updatableFields, false, autoScalingClient, ec2Client, autoScalingGroup, stack);
                }
            }
        }
    }

    private static boolean shouldUpdateLaunchTemplate(List<LaunchTemplateBlockDeviceMapping> currentBlockDeviceMappings, Group group) {
        boolean updateLaunchTemplate = false;
        for (LaunchTemplateBlockDeviceMapping bdm : currentBlockDeviceMappings) {
            if (bdm.ebs() != null) {
                LaunchTemplateEbsBlockDevice ebs = bdm.ebs();
                updateLaunchTemplate = ebs.volumeSize() != group.getRootVolumeSize()
                        || (null != group.getRootVolumeType() && !ebs.volumeTypeAsString().equals(group.getRootVolumeType().toLowerCase(Locale.ROOT)));
            }
        }
        return updateLaunchTemplate;
    }
}
