package com.sequenceiq.redbeams.repository.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;

public class DetailedDBStackStatusConverter extends DefaultEnumConverter<DetailedDBStackStatus> {

    @Override
    public DetailedDBStackStatus getDefault() {
        return DetailedDBStackStatus.AVAILABLE;
    }
}
