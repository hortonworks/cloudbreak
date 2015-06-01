package com.sequenceiq.cloudbreak.cloud.model;

public class Network extends DynamicModel {

    private Subnet subnet;

    public Network(Subnet subnet) {
        this.subnet = subnet;
    }

    public Subnet getSubnet() {
        return subnet;
    }


}
