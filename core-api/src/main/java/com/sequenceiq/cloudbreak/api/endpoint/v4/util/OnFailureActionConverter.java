package com.sequenceiq.cloudbreak.api.endpoint.v4.util;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.OnFailureAction;
import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;

public class OnFailureActionConverter extends DefaultEnumConverter<OnFailureAction> {

    @Override
    public OnFailureAction getDefault() {
        return OnFailureAction.DO_NOTHING;
    }
}
