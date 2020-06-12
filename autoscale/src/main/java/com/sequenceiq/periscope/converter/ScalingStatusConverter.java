package com.sequenceiq.periscope.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.periscope.api.model.ScalingStatus;

public class ScalingStatusConverter extends DefaultEnumConverter<ScalingStatus> {

    @Override
    public ScalingStatus getDefault() {
        return ScalingStatus.SUCCESS;
    }
}
