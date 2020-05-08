package com.sequenceiq.cloudbreak.domain;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;

public class InstanceStatusConverter extends DefaultEnumConverter<InstanceStatus> {

    @Override
    public InstanceStatus getDefault() {
        return InstanceStatus.SERVICES_RUNNING;
    }
}
