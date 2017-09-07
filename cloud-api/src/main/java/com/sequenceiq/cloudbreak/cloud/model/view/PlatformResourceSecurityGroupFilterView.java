package com.sequenceiq.cloudbreak.cloud.model.view;

import java.util.Map;

public class PlatformResourceSecurityGroupFilterView {

    private final String vpcId;

    private final String groupName;

    private final String groupId;

    public PlatformResourceSecurityGroupFilterView(Map<String, String> filters) {
        vpcId = filters.get("vpcId");
        groupName = filters.get("groupName");
        groupId = filters.get("groupId");
    }

    public String getVpcId() {
        return vpcId;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getGroupId() {
        return groupId;
    }
}
