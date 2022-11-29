package com.sequenceiq.periscope.converter.db;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.periscope.api.model.ActivityStatus;

public class ActivityStatusAttributeConverter extends DefaultEnumConverter<ActivityStatus> {

    @Override
    public ActivityStatus getDefault() {
        return ActivityStatus.ACTIVITY_PENDING;
    }
}
