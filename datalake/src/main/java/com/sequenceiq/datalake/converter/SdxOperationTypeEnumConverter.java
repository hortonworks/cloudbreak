package com.sequenceiq.datalake.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.datalake.entity.operation.SdxOperationType;

public class SdxOperationTypeEnumConverter extends DefaultEnumConverter<SdxOperationType> {
    @Override
    public SdxOperationType getDefault() {
        return SdxOperationType.NONE;
    }
}
