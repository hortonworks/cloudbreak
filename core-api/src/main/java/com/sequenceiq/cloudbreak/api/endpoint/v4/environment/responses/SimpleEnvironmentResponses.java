package com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses;

import java.util.HashSet;
import java.util.Set;

import io.swagger.annotations.ApiModel;

@ApiModel
public class SimpleEnvironmentResponses {

    private Set<SimpleEnvironmentV4Response> environments = new HashSet<>();

    public Set<SimpleEnvironmentV4Response> getEnvironments() {
        return environments;
    }

    public void setEnvironments(Set<SimpleEnvironmentV4Response> environments) {
        this.environments = environments;
    }

    public static final SimpleEnvironmentResponses simpleEnvironmentResponses(Set<SimpleEnvironmentV4Response> environments) {
        SimpleEnvironmentResponses simpleEnvironmentResponses = new SimpleEnvironmentResponses();
        simpleEnvironmentResponses.setEnvironments(environments);
        return simpleEnvironmentResponses;
    }
}
