package com.sequenceiq.freeipa.entity.util;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.freeipa.api.model.ResourceStatus;

public class ResourceStatusConverter extends DefaultEnumConverter<ResourceStatus> {

    @Override
    public ResourceStatus getDefault() {
        return ResourceStatus.DEFAULT;
    }
}
