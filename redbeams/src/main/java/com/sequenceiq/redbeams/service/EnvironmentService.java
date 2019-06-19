package com.sequenceiq.redbeams.service;

import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

@Service
public class EnvironmentService {

    @Inject
    private EnvironmentEndpoint environmentEndpoint;

    public DetailedEnvironmentResponse getByCrn(String envCrn) {
        return environmentEndpoint.getByCrn(envCrn);
    }
}
