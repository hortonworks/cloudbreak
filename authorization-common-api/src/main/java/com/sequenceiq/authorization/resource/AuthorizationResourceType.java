package com.sequenceiq.authorization.resource;

import java.util.Arrays;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.authorization.RightsConstants;

public enum AuthorizationResourceType {
    DATALAKE("Datalake cluster", RightsConstants.DATALAKE_RESOURCE),
    ENVIRONMENT("Environment", RightsConstants.ENVIRONMENT_RESOURCE),
    DATAHUB("Datahub cluster", RightsConstants.DATAHUB_RESOURCE);

    private final String readableName;

    private final String authorizationName;

    AuthorizationResourceType(String readableName, String authorizationName) {
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

    public static Optional<AuthorizationResourceType> getByName(String name) {
        return Arrays.stream(AuthorizationResourceType.values())
                .filter(resource -> StringUtils.equals(resource.getAuthorizationName(), name))
                .findAny();
    }
}
