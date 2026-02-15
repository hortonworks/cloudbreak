package com.sequenceiq.cloudbreak.common.notification;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;

public class NotificationStateConverter extends DefaultEnumConverter<NotificationState> {

    @Override
    public NotificationState convertToEntityAttribute(String attribute) {
        return NotificationState.fromStringStateWithDisableIfNull(attribute);
    }

    @Override
    public NotificationState getDefault() {
        return NotificationState.DISABLED;
    }

}
