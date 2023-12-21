package com.sequenceiq.cloudbreak.sdx;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;

public enum TargetPlatform {

    SAAS(CrnResourceDescriptor.SDX_SAAS_INSTANCE),
    PAAS(CrnResourceDescriptor.DATALAKE);

    private static final Logger LOGGER = LoggerFactory.getLogger(TargetPlatform.class);

    private CrnResourceDescriptor crnResourceDescriptor;

    TargetPlatform(CrnResourceDescriptor crnResourceDescriptor) {
        this.crnResourceDescriptor = crnResourceDescriptor;
    }

    public CrnResourceDescriptor getCrnResourceDescriptor() {
        return crnResourceDescriptor;
    }

    public static TargetPlatform getByCrn(String crn) {
        return Arrays.stream(values())
                .filter(platform -> platform.getCrnResourceDescriptor().equals(CrnResourceDescriptor.getByCrnString(crn)))
                .findFirst()
                .orElseGet(() -> {
                    LOGGER.warn("Based on CRN we were not able to decide the target platform, fallback to PAAS");
                    return PAAS;
                });
    }
}
