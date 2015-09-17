package com.sequenceiq.cloudbreak.cloud.openstack.nativ;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.PlatformParameters;

@Service
public class OpenStackNativeParameters implements PlatformParameters {

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
