package com.sequenceiq.notification.domain.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.notification.domain.NotificationFormFactor;

public class NotificationFormFactorConverter extends DefaultEnumConverter<NotificationFormFactor> {

    @Override
    public NotificationFormFactor getDefault() {
        return NotificationFormFactor.DISTRIBUTION_LIST;
    }
}
