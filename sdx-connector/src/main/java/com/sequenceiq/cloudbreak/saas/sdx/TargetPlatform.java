package com.sequenceiq.cloudbreak.saas.sdx;

import java.util.Arrays;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;

public enum TargetPlatform {
    SAAS(CrnResourceDescriptor.SDX_SAAS_INSTANCE),
    PAAS(CrnResourceDescriptor.DATALAKE);

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
                .orElseThrow(() -> new NotFoundException(String.format("There is no SDX platform based on CRN %s", crn)));
    }
}
