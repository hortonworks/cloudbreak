package com.sequenceiq.environment.environment.service;

import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.START_FREEIPA_DELETE_EVENT;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.DatalakeV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.exception.EnvironmentServiceException;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;

import reactor.bus.Event;

@Service
public class EnvironmentResourceDeletionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentResourceDeletionService.class);

    private final SdxEndpoint sdxEndpoint;

    private final DatalakeV4Endpoint datalakeV4Endpoint;

    private final DistroXV1Endpoint distroXV1Endpoint;

    private final ThreadBasedUserCrnProvider userCrnProvider;

    private final EventSender eventSender;

    public EnvironmentResourceDeletionService(SdxEndpoint sdxEndpoint, DatalakeV4Endpoint datalakeV4Endpoint, DistroXV1Endpoint distroXV1Endpoint,
            ThreadBasedUserCrnProvider userCrnProvider, EventSender eventSender) {
        this.sdxEndpoint = sdxEndpoint;
        this.datalakeV4Endpoint = datalakeV4Endpoint;
        this.distroXV1Endpoint = distroXV1Endpoint;
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

    Set<String> getDatalakeClusterNames(Environment environment) {
        Set<String> clusterNames = new HashSet<>();
        LOGGER.debug("Get Datalake clusters of the environment: '{}'", environment.getName());
        try {
            Set<String> datalakeClusterNames = datalakeV4Endpoint
                    .list(environment.getName())
                    .getResponses()
                    .stream()
                    .map(StackViewV4Response::getName)
                    .collect(Collectors.toSet());
            clusterNames.addAll(datalakeClusterNames);
        } catch (WebApplicationException | ProcessingException e) {
            String message = String.format("Failed to get Datalake clusters from Cloudbreak service due to: '%s' ", e.getMessage());
            LOGGER.error(message, e);
            throw new EnvironmentServiceException(message, e);
        }
        return clusterNames;
    }

    Set<String> getAttachedDistroXClusterNames(Environment environment) {
        Set<String> clusterNames = new HashSet<>();
        LOGGER.debug("Get DistroX clusters of the environment: '{}'", environment.getName());
        try {
            Set<String> distroXClusterNames = distroXV1Endpoint
                    .list(environment.getName(), environment.getResourceCrn())
                    .getResponses()
                    .stream()
                    .map(StackViewV4Response::getName)
                    .collect(Collectors.toSet());
            clusterNames.addAll(distroXClusterNames);
        } catch (WebApplicationException | ProcessingException e) {
            String message = String.format("Failed to get DistroX clusters from Cloudbreak service due to: '%s' ", e.getMessage());
            LOGGER.error(message, e);
            throw new EnvironmentServiceException(message, e);
        }
        return clusterNames;
    }

    void triggerDeleteFlow(Environment environment) {
        EnvDeleteEvent envDeleteEvent = EnvDeleteEvent.EnvDeleteEventBuilder.anEnvDeleteEvent()
                .withSelector(START_FREEIPA_DELETE_EVENT.selector())
                .withResourceId(environment.getId())
                .withResourceName(environment.getName())
                .build();
        String userCrn = userCrnProvider.getUserCrn();
        if (StringUtils.isEmpty(userCrn)) {
            LOGGER.warn("Delete flow is about to start, but user crn is not available!");
        }
        Map<String, Object> flowTriggerUsercrn = Map.of(FlowConstants.FLOW_TRIGGER_USERCRN, userCrn);
        eventSender.sendEvent(envDeleteEvent, new Event.Headers(flowTriggerUsercrn));
    }

}
