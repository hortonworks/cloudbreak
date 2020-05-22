package com.sequenceiq.redbeams.repository.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.redbeams.api.model.common.Status;

public class StatusConverter extends DefaultEnumConverter<Status> {

    @Override
    public Status getDefault() {
        return Status.AVAILABLE;
    }
}
