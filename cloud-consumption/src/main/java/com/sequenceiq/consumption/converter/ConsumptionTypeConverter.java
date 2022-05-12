package com.sequenceiq.consumption.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.consumption.api.v1.consumption.model.common.ConsumptionType;

public class ConsumptionTypeConverter extends DefaultEnumConverter<ConsumptionType> {

    @Override
    public ConsumptionType getDefault() {
        return ConsumptionType.UNKNOWN;
    }
}
