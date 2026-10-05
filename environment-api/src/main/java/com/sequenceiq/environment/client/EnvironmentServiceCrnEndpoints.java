package com.sequenceiq.environment.client;

import jakarta.ws.rs.client.WebTarget;

import com.sequenceiq.cloudbreak.client.AbstractUserCrnServiceEndpoint;

public class EnvironmentServiceCrnEndpoints extends AbstractUserCrnServiceEndpoint implements EnvironmentClient {

    EnvironmentServiceCrnEndpoints(WebTarget webTarget, String crn) {
        super(webTarget, crn);
    }
}

