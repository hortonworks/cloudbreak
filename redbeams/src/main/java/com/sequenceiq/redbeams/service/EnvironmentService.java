package com.sequenceiq.redbeams.service;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.authorization.service.EnvironmentPropertyProvider;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v2.environment.endpoint.EnvironmentV2Endpoint;

@Service
public class EnvironmentService implements EnvironmentPropertyProvider {

    @Inject
    private EnvironmentV2Endpoint environmentEndpoint;

    public DetailedEnvironmentResponse getByCrn(String envCrn) {
        return environmentEndpoint.getByCrn(envCrn);
    }

}
