package com.sequenceiq.cloudbreak.domain;

import com.sequenceiq.cloudbreak.controller.json.JsonEntity;

public class SubnetJson implements JsonEntity {

    private String subnet;

    public SubnetJson() {
    }

    public SubnetJson(String subnet) {
        this.subnet = subnet;
    }

    public String getSubnet() {
        return subnet;
    }

    public void setSubnet(String subnet) {
        this.subnet = subnet;
    }
}
