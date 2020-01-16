package com.sequenceiq.it.cloudbreak.dto.mock;

class SparkUriParameters {
    private final String uri;

    private final Class type;

    SparkUriParameters(String uri, Class type) {
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
