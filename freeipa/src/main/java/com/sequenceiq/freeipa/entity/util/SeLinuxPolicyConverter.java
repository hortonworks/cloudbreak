package com.sequenceiq.freeipa.entity.util;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.common.model.SeLinuxPolicy;

public class SeLinuxPolicyConverter extends DefaultEnumConverter<SeLinuxPolicy> {

    @Override
    public SeLinuxPolicy getDefault() {
        return SeLinuxPolicy.PERMISSIVE;
    }
}
