package com.sequenceiq.flow.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.flow.domain.StateStatus;

public class StateStatusConverter extends DefaultEnumConverter<StateStatus> {

    @Override
    public StateStatus getDefault() {
        return StateStatus.SUCCESSFUL;
    }
}
