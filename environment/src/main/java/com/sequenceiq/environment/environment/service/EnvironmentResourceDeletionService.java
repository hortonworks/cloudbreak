package com.sequenceiq.environment.environment.service;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.exception.EnvironmentServiceException;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;

@Service
public class EnvironmentResourceDeletionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentResourceDeletionService.class);

    private final SdxEndpoint sdxEndpoint;

    private final ThreadBasedUserCrnProvider userCrnProvider;

    private final EventSender eventSender;

    public EnvironmentResourceDeletionService(SdxEndpoint sdxEndpoint, ThreadBasedUserCrnProvider userCrnProvider, EventSender eventSender) {
        this.sdxEndpoint = sdxEndpoint;
        this.userCrnProvider = userCrnProvider;
        this.eventSender = eventSender;
    }

    Set<String> getAttachedSdxClusterNames(Environment environment) {
        Set<String> clusterNames = new HashSet<>();
        LOGGER.debug("Get SDX clusters of the environment: '{}'", environment.getName());
        try {
            Set<String> sdxClusterNames = sdxEndpoint
                    .list(environment.getName())
                    .stream()
                    .map(SdxClusterResponse::getName)
                    .collect(Collectors.toSet());
            clusterNames.addAll(sdxClusterNames);
        } catch (WebApplicationException | ProcessingException e) {
            String message = String.format("Failed to get SDX clusters from SDX service due to: '%s' ", e.getMessage());
            LOGGER.error(message, e);
            throw new EnvironmentServiceException(message, e);
        }
        return clusterNames;
    }
}
