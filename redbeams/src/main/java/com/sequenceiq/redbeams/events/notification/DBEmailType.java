package com.sequenceiq.redbeams.events.notification;

public enum DBEmailType {
    EXTERNAL_DATABASE_STORAGE_ALERT(
            "is approaching its allocated storage limit",
            "External Database Storage",
            "STORAGE_CHECK",
            "Capacity Alert"
    );

    private final String operationMessage;

    private final String operation;

    private final String status;

    private final String mainTitle;

    DBEmailType(String operationMessage, String operation, String status, String mainTitle) {
        this.operationMessage = operationMessage;
        this.operation = operation;
        this.status = status;
        this.mainTitle = mainTitle;
    }

    public String operationMessage() {
        return operationMessage;
    }

    public String operation() {
        return operation;
    }

    public String status() {
        return status;
    }

    public String mainTitle() {
        return mainTitle;
    }
}
