package com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses;

import java.util.HashSet;
import java.util.Set;

import io.swagger.annotations.ApiModel;

@ApiModel
public class SimpleEnvironmentV4Responses {

    private Set<SimpleEnvironmentV4Response> environments = new HashSet<>();

    public Set<SimpleEnvironmentV4Response> getEnvironments() {
        return environments;
    }

    public void setEnvironments(Set<SimpleEnvironmentV4Response> environments) {
        this.environments = environments;
    }

    public static final SimpleEnvironmentV4Responses simpleEnvironmentResponses(Set<SimpleEnvironmentV4Response> environments) {
        SimpleEnvironmentV4Responses simpleEnvironmentv4Responses = new SimpleEnvironmentV4Responses();
        simpleEnvironmentv4Responses.setEnvironments(environments);
        return simpleEnvironmentv4Responses;
    }
}
