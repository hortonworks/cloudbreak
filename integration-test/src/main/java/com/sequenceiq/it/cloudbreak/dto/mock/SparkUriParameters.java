package com.sequenceiq.it.cloudbreak.dto.mock;

public class SparkUriParameters {
    private final String uri;

    private final Class type;

    public SparkUriParameters(String uri, Class type) {
        this.uri = uri;
        this.type = type;
    }

    public String getUri() {
        return uri;
    }

    public Class getType() {
        return type;
    }
}
