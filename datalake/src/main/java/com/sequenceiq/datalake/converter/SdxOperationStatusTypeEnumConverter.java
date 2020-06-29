package com.sequenceiq.datalake.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.datalake.entity.operation.SdxOperationStatus;

public class SdxOperationStatusTypeEnumConverter extends DefaultEnumConverter<SdxOperationStatus> {

    @Override
    public SdxOperationStatus getDefault() {
        return SdxOperationStatus.INIT;
    }
}
