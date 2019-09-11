package com.sequenceiq.redbeams.service;

import javax.inject.Inject;

import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Service
public class EnvironmentService {

    @Inject
    private RetryTemplate cbRetryTemplate;

    @Inject
    private EnvironmentEndpoint environmentEndpoint;

    public DetailedEnvironmentResponse getByCrn(String envCrn) {
        return cbRetryTemplate.execute(rctx -> environmentEndpoint.getByCrn(envCrn));
    }
}
