package com.sequenceiq.sdx.api.model;

public class ModelDescriptions {

    public static final String RECOVERY_TYPE = "Type of the recovery operation, automatic restore is performed in case of 'RECOVER_WITH_DATA'";

    public static final String BACKUP_ID = "The ID of the database backup.";

    public static final String BACKUP_LOCATION = "The location where the database backup will be stored.";

    public static final String CLOSE_CONNECTIONS = "The conditional parameter for whether connections to the database will be closed during backup or not.";

    private ModelDescriptions() {
    }
}
