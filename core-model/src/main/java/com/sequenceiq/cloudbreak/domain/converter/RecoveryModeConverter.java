package com.sequenceiq.cloudbreak.domain.converter;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RecoveryMode;
import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;

public class RecoveryModeConverter extends DefaultEnumConverter<RecoveryMode> {

    @Override
    public RecoveryMode getDefault() {
        return RecoveryMode.AUTO;
    }
}
