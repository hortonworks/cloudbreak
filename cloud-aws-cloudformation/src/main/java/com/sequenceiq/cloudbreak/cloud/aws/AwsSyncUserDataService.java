package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.collections4.MapUtils;
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
            if (!cloudStack.getUserDataByType(group.getType()).equals(launchTemplateUserData.get(group.getName()))) {
                LOGGER.info("User data in Launch template differs from database content for group '{}'", group.getName());
                updateNeeded = true;
            }
        }
        return updateNeeded;
    }

    private Map<InstanceGroupType, String> getUserDataMap(CloudStack cloudStack) {
        return Map.of(
                InstanceGroupType.GATEWAY, cloudStack.getGatewayUserData(),
                InstanceGroupType.CORE, cloudStack.getCoreUserData()
        );
    }

}
