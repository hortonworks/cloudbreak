package com.sequenceiq.notification.domain;

import java.util.Locale;

/**
 * Represents the action that triggered a distribution list create-or-update request.
 * The action type determines whether an existing user-managed distribution list may be
 * overwritten: during REGISTRATION (resource creation) the list is always updated,
 * whereas for other actions a user-managed list is left untouched.
 */
public enum DistributionListActionType {
    REGISTRATION;

    public static DistributionListActionType fromString(String actionType) {
        return DistributionListActionType.valueOf(actionType.toUpperCase(Locale.ROOT));
    }
}
