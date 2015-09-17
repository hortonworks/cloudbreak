package com.sequenceiq.cloudbreak.cloud.openstack;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.PlatformParameters;

@Service
public class OpenStackParameters implements PlatformParameters {
    private static final Integer START_LABEL = Integer.valueOf(97);

    @Override
    public String diskPrefix() {
        return "vd";
    }

    @Override
    public Integer startLabel() {
        return START_LABEL;
    }
}
