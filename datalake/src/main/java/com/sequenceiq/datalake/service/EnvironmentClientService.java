package com.sequenceiq.datalake.service;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionHandler;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Service
public class EnvironmentClientService {

    @Inject
    private EnvironmentEndpoint environmentEndpoint;

    @Inject
    private WebApplicationExceptionHandler webApplicationExceptionHandler;

    public DetailedEnvironmentResponse getByName(String environment) {
        try {
            return environmentEndpoint.getByName(environment);
        } catch (WebApplicationException e) {
            throw webApplicationExceptionHandler.handleException(e);
        }
    }

    public DetailedEnvironmentResponse getByCrn(String environmentCrn) {
        try {
            return environmentEndpoint.getByCrn(environmentCrn);
        } catch (WebApplicationException e) {
            throw webApplicationExceptionHandler.handleException(e);
        }
    }
}
