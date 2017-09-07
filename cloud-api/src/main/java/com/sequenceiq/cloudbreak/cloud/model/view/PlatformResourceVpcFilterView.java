package com.sequenceiq.cloudbreak.cloud.model.view;

import java.util.Map;

public class PlatformResourceVpcFilterView {

    private final String vpcId;

    public PlatformResourceVpcFilterView(Map<String, String> filters) {
        vpcId = filters.get("vpcId");
    }

    public String getVpcId() {
        return vpcId;
    }
}
