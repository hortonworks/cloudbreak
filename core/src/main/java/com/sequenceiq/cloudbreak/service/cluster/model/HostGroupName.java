package com.sequenceiq.cloudbreak.service.cluster.model;

import java.util.Objects;

import com.sequenceiq.cloudbreak.cloud.model.generic.StringType;

public class HostGroupName extends StringType {

    private HostGroupName(String name) {
        super(name);
    }

    public static HostGroupName hostGroupName(String hostGroupName) {
        Objects.requireNonNull(hostGroupName);
        return new HostGroupName(hostGroupName);
    }
}
