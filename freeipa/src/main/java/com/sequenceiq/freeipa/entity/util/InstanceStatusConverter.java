package com.sequenceiq.freeipa.entity.util;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;

public class InstanceStatusConverter extends DefaultEnumConverter<InstanceStatus> {

    @Override
    public InstanceStatus getDefault() {
        return InstanceStatus.CREATED;
    }
}
