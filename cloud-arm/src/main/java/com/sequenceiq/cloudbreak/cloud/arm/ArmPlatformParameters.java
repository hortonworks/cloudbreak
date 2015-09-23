package com.sequenceiq.cloudbreak.cloud.arm;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.PlatformParameters;

@Service
public class ArmPlatformParameters implements PlatformParameters {

    private static final int START_LABEL = 98;

    @Override
    public String diskPrefix() {
        return "sd";
    }

    @Override
    public Integer startLabel() {
        return START_LABEL;
    }
}
