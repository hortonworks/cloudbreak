package com.sequenceiq.cloudbreak.cloud.gcp;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.PlatformParameters;

@Service
public class GcpPlatformParameters implements PlatformParameters {
    private static final Integer START_LABEL = Integer.valueOf(97);

    @Override
    public String diskPrefix() {
        return "sd";
    }

    @Override
    public Integer startLabel() {
        return START_LABEL;
    }
}
