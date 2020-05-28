package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.common.api.type.CommonStatus;

public class CommonStatusConverter extends DefaultEnumConverter<CommonStatus> {

    @Override
    public CommonStatus getDefault() {
        return CommonStatus.CREATED;
    }
}
