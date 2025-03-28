package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.common.model.ProviderSyncState;

public class ProviderSyncStateConverter extends DefaultEnumConverter<ProviderSyncState> {

    @Override
    public ProviderSyncState getDefault() {
        return ProviderSyncState.VALID;
    }
}