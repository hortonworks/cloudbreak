package com.sequenceiq.periscope.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.periscope.api.model.AlertType;

public class AlertTypeConverter extends DefaultEnumConverter<AlertType> {

    @Override
    public AlertType getDefault() {
        return AlertType.METRIC;
    }
}
