package com.sequenceiq.cloudbreak.cloud.model.view;

import java.util.Map;

public class PlatformResourceVpcFilterView {

    private String vpcId;

    public PlatformResourceVpcFilterView(Map<String, String> filters) {
        this.vpcId = filters.get("vpcId");
    }

    public String getVpcId() {
        return vpcId;
    }
}
