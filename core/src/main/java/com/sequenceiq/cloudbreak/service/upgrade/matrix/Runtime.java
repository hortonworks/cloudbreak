package com.sequenceiq.cloudbreak.service.upgrade.matrix;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Runtime {

    private final String version;

    @JsonCreator
    public Runtime(@JsonProperty("version") String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }
}
