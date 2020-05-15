package com.sequenceiq.freeipa.service.client;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionHandler;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Service
public class CachedEnvironmentClientService {

    @Inject
    private EnvironmentEndpoint environmentEndpoint;

    @Inject
    private WebApplicationExceptionHandler webApplicationExceptionHandler;

    @Cacheable(cacheNames = "freeipaEnvironmentCache", key = "#environment")
    public DetailedEnvironmentResponse getByName(String environment) {
        try {
            return environmentEndpoint.getByName(environment);
        } catch (WebApplicationException e) {
            throw webApplicationExceptionHandler.handleException(e);
        }
    }

    @Cacheable(cacheNames = "freeipaEnvironmentCache", key = "#environmentCrn")
    public DetailedEnvironmentResponse getByCrn(String environmentCrn) {
        try {
            return environmentEndpoint.getByCrn(environmentCrn);
        } catch (WebApplicationException e) {
            throw webApplicationExceptionHandler.handleException(e);
        }
    }

    @CacheEvict(value = "freeipaEnvironmentCache", key = "#environmentCrn")
    public void evictCache(String environmentCrn) {
    }
}
