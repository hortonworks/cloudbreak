package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.AwsUpdateService;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.common.api.type.InstanceGroupType;

@Service
public class AwsSyncUserDataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsSyncUserDataService.class);

    @Inject
    private AwsUserDataService awsUserDataService;

    @Inject
    private AwsUpdateService awsUpdateService;

    public void syncUserData(AuthenticatedContext ac, CloudStack cloudStack, List<CloudResource> resources) {
        LOGGER.debug("Syncing userdata for stack {}", ac.getCloudContext().getName());
        boolean updateNeeded = isUserDataUpdateNeeded(ac, cloudStack);
        if (updateNeeded) {
            LOGGER.info("User data in at least one Launch template differs from database, update is needed.");
            Map<InstanceGroupType, String> userData = getUserDataMap(cloudStack);
            awsUpdateService.updateUserData(ac, cloudStack, resources, userData);
        } else {
            LOGGER.info("User data is the same as stored in the database, update is not needed.");
        }
    }

    private boolean isUserDataUpdateNeeded(AuthenticatedContext ac, CloudStack cloudStack) {
        boolean updateNeeded = false;
        Map<String, String> launchTemplateUserData = awsUserDataService.getUserData(ac, cloudStack);

        if (MapUtils.isEmpty(launchTemplateUserData)) {
            LOGGER.warn("Launch Template user data is empty for stack {}", ac.getCloudContext().getName());
            return false;
        }

        for (Group group : cloudStack.getGroups()) {
            String userDataByGroupType = cloudStack.getUserDataByType(group.getType());
            if (StringUtils.isNotEmpty(userDataByGroupType)) {
                String userDataFromLaunchTemplate = launchTemplateUserData.get(group.getName());
                String trimmedUserDataFromLaunchTemplate = userDataFromLaunchTemplate != null ? userDataFromLaunchTemplate.trim() : null;
                if (!userDataByGroupType.trim().equals(trimmedUserDataFromLaunchTemplate)) {
                    LOGGER.info("User data in Launch template differs from database content for group '{}'", group.getName());
                    updateNeeded = true;
                }
            } else {
                String msg = String.format("The user data is empty for group '%s' and group type '%s' which should not happen!",
                        group.getName(), group.getType());
                LOGGER.warn(msg);
                throw new IllegalStateException(msg);
            }
        }
        return updateNeeded;
    }

    private Map<InstanceGroupType, String> getUserDataMap(CloudStack cloudStack) {
        Map<InstanceGroupType, String> result = new HashMap<>();
        putUserDataByTypeIfNotEmpty(result, InstanceGroupType.GATEWAY, cloudStack.getGatewayUserData());
        putUserDataByTypeIfNotEmpty(result, InstanceGroupType.CORE, cloudStack.getCoreUserData());
        return result;
    }

    private static void putUserDataByTypeIfNotEmpty(Map<InstanceGroupType, String> result, InstanceGroupType groupType, String userData) {
        if (StringUtils.isNotEmpty(userData)) {
            result.put(groupType, userData);
        }
    }

}
