package com.sequenceiq.authorization.resource;

public enum AuthorizationResource {
    DATALAKE("Datalake cluster", "datalake"),
    ENVIRONMENT("Environment", "environment"),
    DATAHUB("Datahub cluster", "datahub");

    private final String readableName;

    private final String authorizationName;

    AuthorizationResource(String readableName, String authorizationName) {
        this.readableName = readableName;
        this.authorizationName = authorizationName;
    }

    public String getReadableName() {
        return readableName;
    }

    public String getShortName() {
        return authorizationName.toLowerCase();
    }

    public String getAuthorizationName() {
        return authorizationName;
    }
}
