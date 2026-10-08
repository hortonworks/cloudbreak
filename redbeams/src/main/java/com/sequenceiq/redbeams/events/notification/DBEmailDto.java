package com.sequenceiq.redbeams.events.notification;

public record DBEmailDto(DBEmailType dbEmailType) {

    public static DBEmailDto storageAlert() {
        return new DBEmailDto(DBEmailType.EXTERNAL_DATABASE_STORAGE_ALERT);
    }
}
