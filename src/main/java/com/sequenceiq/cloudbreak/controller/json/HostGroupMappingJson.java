package com.sequenceiq.cloudbreak.controller.json;

public class HostGroupMappingJson {

    private String name;
    private int cardinality;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCardinality() {
        return cardinality;
    }

    public void setCardinality(int cardinality) {
        this.cardinality = cardinality;
    }

}
