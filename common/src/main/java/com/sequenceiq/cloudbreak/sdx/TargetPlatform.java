package com.sequenceiq.cloudbreak.sdx;

import java.util.Arrays;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;

public enum TargetPlatform {

    PAAS(CrnResourceDescriptor.VM_DATALAKE),
    PDL(CrnResourceDescriptor.ENVIRONMENT, CrnResourceDescriptor.CLASSIC_CLUSTER);

    private static final Logger LOGGER = LoggerFactory.getLogger(TargetPlatform.class);

    private final Set<CrnResourceDescriptor> crnResourceDescriptors;

    TargetPlatform(CrnResourceDescriptor... crnResourceDescriptors) {
        this.crnResourceDescriptors = Set.of(crnResourceDescriptors);
    }

    public static TargetPlatform getByCrn(String crn) {
        CrnResourceDescriptor crnResourceDescriptor = CrnResourceDescriptor.getByCrnString(crn);
        return Arrays.stream(values())
                .filter(platform -> platform.crnResourceDescriptors.contains(crnResourceDescriptor))
                .findFirst()
                .orElseGet(() -> {
                    LOGGER.warn("Based on CRN we were not able to decide the target platform, fallback to PAAS");
                    return PAAS;
                });
    }
}
