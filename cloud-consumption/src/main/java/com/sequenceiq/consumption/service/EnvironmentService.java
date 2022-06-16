package com.sequenceiq.consumption.service;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionHandler;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Service
public class EnvironmentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentService.class);

    @Inject
    private EnvironmentEndpoint environmentEndpoint;

    @Inject
    private WebApplicationExceptionHandler webApplicationExceptionHandler;

    public DetailedEnvironmentResponse getByCrn(String environmentCrn) {
        try {
            LOGGER.debug("Getting environment by crn '{}'", environmentCrn);
            return environmentEndpoint.getByCrn(environmentCrn);
        } catch (WebApplicationException e) {
            LOGGER.error("Could not get environment by crn '{}'. Reason: {}", environmentCrn, e.getMessage(), e);
            throw webApplicationExceptionHandler.handleException(e);
        }
    } }
