package com.sequenceiq.cloudbreak.cloud.openstack;

import java.util.Map;

import org.springframework.stereotype.Component;

@Component("OpenStackUtilV2")
public class OpenStackUtil {

    public static final String OPENSTACK = "OPENSTACK";

    public static final String CB_INSTANCE_GROUP_NAME = "cb_instance_group_name";
    public static final String CB_INSTANCE_PRIVATE_ID = "cb_instance_private_id";

    public String getInstanceId(String uuid, Map<String, String> metadata) {
        return uuid + "_" + getNormalizedGroupName(metadata.get(CB_INSTANCE_GROUP_NAME)) + "_"
                + metadata.get(CB_INSTANCE_PRIVATE_ID);
    }

    public String getNormalizedGroupName(String groupName) {
        return groupName.replaceAll("_", "");
    }

}
