package com.sequenceiq.environment.environment.service;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.exception.EnvironmentServiceException;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;

@Service
public class EnvironmentResourceDeletionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentResourceDeletionService.class);

    private final SdxEndpoint sdxEndpoint;

    public EnvironmentResourceDeletionService(SdxEndpoint sdxEndpoint) {
        this.sdxEndpoint = sdxEndpoint;
    }

    public Set<String> getAttachedSdxClusterNames(Environment environment) {
        Set<String> clusterNames = new HashSet<>();
        LOGGER.debug("Get SDX clusters of the environment: '{}'", environment.getName());
        try {
            Set<String> sdxClusterNames = sdxEndpoint
                    .list(environment.getName())
                    .stream()
                    .map(SdxClusterResponse::getName)
                    .collect(Collectors.toSet());
            clusterNames.addAll(sdxClusterNames);
        } catch (WebApplicationException e) {
            String message = String.format("Failed to get SDX clusters from SDX service due to: '%s' ", e.getMessage());
            LOGGER.error(message, e);
            throw new EnvironmentServiceException(message, e);
        }
        return clusterNames;
    }

    public void deleteResources(Environment environment) {

    }
}
