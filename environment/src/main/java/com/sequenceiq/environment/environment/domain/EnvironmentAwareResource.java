package com.sequenceiq.environment.environment.domain;

import java.util.Set;

import com.sequenceiq.cloudbreak.auth.security.AuthResource;

public interface EnvironmentAwareResource extends AuthResource {

    String getName();

    void setName(String name);

    Set<EnvironmentView> getEnvironments();

    void setEnvironments(Set<EnvironmentView> environments);
}
