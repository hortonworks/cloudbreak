package com.sequenceiq.cloudbreak.domain;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;

public class DetailedStackStatusConverter extends DefaultEnumConverter<DetailedStackStatus> {

    @Override
    public DetailedStackStatus getDefault() {
        return DetailedStackStatus.AVAILABLE;
    }
}
