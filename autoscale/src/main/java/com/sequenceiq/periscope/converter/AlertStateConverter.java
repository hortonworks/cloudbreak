package com.sequenceiq.periscope.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.periscope.api.model.AlertState;

public class AlertStateConverter extends DefaultEnumConverter<AlertState> {

    @Override
    public AlertState getDefault() {
        return AlertState.OK;
    }
}
