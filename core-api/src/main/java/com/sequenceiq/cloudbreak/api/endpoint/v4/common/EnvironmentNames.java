package com.sequenceiq.cloudbreak.api.endpoint.v4.common;

import javax.validation.constraints.NotEmpty;
import java.util.Set;

public class EnvironmentNames {

    @NotEmpty
    private Set<String> environmentNames;

    public Set<String> getEnvironmentNames() {
        return environmentNames;
    }

    public void setEnvironmentNames(Set<String> environmentNames) {
        this.environmentNames = environmentNames;
    }
}
