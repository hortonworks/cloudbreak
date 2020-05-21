package com.sequenceiq.cloudbreak.domain.converter;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;

public class StatusConverter extends DefaultEnumConverter<Status> {

    @Override
    public Status getDefault() {
        return Status.AVAILABLE;
    }
}
