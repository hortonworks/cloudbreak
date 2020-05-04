package com.sequenceiq.authorization.resource;

import java.util.Arrays;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

public enum AuthorizationResourceType {
    DATALAKE("datalake"),
    ENVIRONMENT("environments"),
    CREDENTIAL("environments"),
    DATAHUB("datahub"),
    IMAGE_CATALOG("environments");

    private final String resource;

    AuthorizationResourceType(String resource) {
        this.resource = resource;
    }

    public String getResource() {
        return resource;
    }

    public static Optional<AuthorizationResourceType> getByName(String name) {
        return Arrays.stream(AuthorizationResourceType.values())
                .filter(resource -> StringUtils.equals(resource.getResource(), name))
                .findAny();
    }
}
