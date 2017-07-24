package com.sequenceiq.cloudbreak.cloud.model.view;

import java.util.Map;

public class PlatformResourceSecurityGroupFilterView {

    private String vpcId;

    private String groupName;

    private String groupId;

    public PlatformResourceSecurityGroupFilterView(Map<String, String> filters) {
        this.vpcId = filters.get("vpcId");
        this.groupName = filters.get("groupName");
        this.groupId = filters.get("groupId");
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
