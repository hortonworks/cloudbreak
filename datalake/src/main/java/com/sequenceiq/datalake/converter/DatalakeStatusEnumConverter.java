package com.sequenceiq.datalake.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;

public class DatalakeStatusEnumConverter extends DefaultEnumConverter<DatalakeStatusEnum> {

    @Override
    public DatalakeStatusEnum getDefault() {
        return DatalakeStatusEnum.RUNNING;
    }
}
