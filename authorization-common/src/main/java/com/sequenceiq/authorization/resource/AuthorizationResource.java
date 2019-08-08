package com.sequenceiq.authorization.resource;

import java.util.Arrays;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

public enum AuthorizationResource {
    DATALAKE("Datalake cluster", "datalake"),
    ENVIRONMENT("Environment", "environments"),
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

    public static Optional<AuthorizationResource> getByName(String name) {
        return Arrays.stream(AuthorizationResource.values())
                .filter(resource -> StringUtils.equals(resource.getAuthorizationName(), name))
                .findAny();
    }
}
