package com.sequenceiq.redbeams.repository.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.redbeams.api.endpoint.v4.ResourceStatus;

public class ResourceStatusConverter extends DefaultEnumConverter<ResourceStatus> {

    @Override
    public ResourceStatus getDefault() {
        return ResourceStatus.UNKNOWN;
    }
}
