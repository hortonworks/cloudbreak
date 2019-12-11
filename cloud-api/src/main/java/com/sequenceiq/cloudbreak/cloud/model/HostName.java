package com.sequenceiq.cloudbreak.cloud.model;

import com.sequenceiq.cloudbreak.cloud.model.generic.StringType;

public class HostName extends StringType implements Comparable<HostName> {
    private HostName(String value) {
        super(value);
    }

    public static HostName hostName(String value) {
        return new HostName(value);
    }

    @Override
    public int compareTo(HostName o) {
        return value().compareTo(o.value());
    }
}
