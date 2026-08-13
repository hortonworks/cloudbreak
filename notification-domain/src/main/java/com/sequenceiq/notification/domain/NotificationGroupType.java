package com.sequenceiq.notification.domain;

import com.sequenceiq.cloudbreak.auth.crn.Crn;

// Used to define which level of distribution list required for the notification
public enum NotificationGroupType {
    ENVIRONMENT, DATAHUB, DATALAKE;

    public static NotificationGroupType byCrn(String crnString) {
        if (Crn.isCrn(crnString)) {
            Crn crn = Crn.fromString(crnString);
            switch (crn.getService()) {
                case Crn.Service.DATAHUB:
                    return NotificationGroupType.DATAHUB;
                case Crn.Service.DATALAKE:
                    return NotificationGroupType.DATALAKE;
                default:
                    return NotificationGroupType.ENVIRONMENT;
            }
        }
        return ENVIRONMENT;
    }
}
