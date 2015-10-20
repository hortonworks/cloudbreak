package com.sequenceiq.cloudbreak.cloud.model;

import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel;

public class Network extends DynamicModel {

    private final Subnet subnet;

    public Network(Subnet subnet) {
        this.subnet = subnet;
    }

    public Subnet getSubnet() {
        return subnet;
    }


}
