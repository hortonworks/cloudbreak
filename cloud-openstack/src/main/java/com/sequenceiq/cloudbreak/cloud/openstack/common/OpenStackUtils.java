package com.sequenceiq.cloudbreak.cloud.openstack.common;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;

@Component
public class OpenStackUtils {

    public static final String CB_INSTANCE_GROUP_NAME = "cb_instance_group_name";

    public static final String CB_INSTANCE_PRIVATE_ID = "cb_instance_private_id";

    public String getPrivateInstanceId(String groupName, String privateId) {
        return getNormalizedGroupName(groupName) + '_' + privateId;
    }

    public String getPrivateInstanceId(Map<String, String> metadata) {
        return getPrivateInstanceId(metadata.get(CB_INSTANCE_GROUP_NAME), metadata.get(CB_INSTANCE_PRIVATE_ID));
    }

    public String getNormalizedGroupName(String groupName) {
        return groupName.replaceAll("_", "");
    }

    public String getStackName(AuthenticatedContext context) {
        return context.getCloudContext().getName() + '_' + context.getCloudContext().getId();
    }
}
