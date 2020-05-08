package com.sequenceiq.cloudbreak.domain.converter;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;

public class ResourceStatusConverter  extends DefaultEnumConverter<ResourceStatus> {

    @Override
    public ResourceStatus getDefault() {
        return ResourceStatus.USER_MANAGED;
    }
}
