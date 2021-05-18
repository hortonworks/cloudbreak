package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;

public class TemporaryStorageConverter extends DefaultEnumConverter<TemporaryStorage> {

    @Override
    public TemporaryStorage getDefault() {
        return TemporaryStorage.ATTACHED_VOLUMES;
    }

}
