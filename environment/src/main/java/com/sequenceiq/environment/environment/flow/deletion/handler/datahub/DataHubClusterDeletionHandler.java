package com.sequenceiq.environment.environment.flow.deletion.handler.datahub;

import static com.sequenceiq.environment.environment.flow.deletion.event.EnvClustersDeleteStateSelectors.START_DATALAKE_CLUSTERS_DELETE_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_DATAHUB_CLUSTERS_EVENT;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteFailedEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.util.PollingConfig;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;

@Component
public class DataHubClusterDeletionHandler extends EventSenderAwareHandler<EnvironmentDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataHubClusterDeletionHandler.class);

    private static final int SLEEP_TIME = 10;

    private static final int TIMEOUT = 1;

    private final EnvironmentService environmentService;

    private final DatahubDeletionService datahubDeletionService;

    protected DataHubClusterDeletionHandler(EventSender eventSender, EnvironmentService environmentService, DatahubDeletionService datahubDeletionService) {
        super(eventSender);
        this.environmentService = environmentService;
        this.datahubDeletionService = datahubDeletionService;
    }

    @Override
    public void accept(Event<EnvironmentDto> environmentDtoEvent) {
        LOGGER.debug("Accepting DataHubClustersDelete event");
        EnvironmentDto environmentDto = environmentDtoEvent.getData();
        try {
            PollingConfig pollingConfig = getPollingConfig();
            environmentService.findEnvironmentById(environmentDto.getId())
                    .ifPresent(environment -> datahubDeletionService.deleteDatahubClustersForEnvironment(pollingConfig, environment));
            EnvDeleteEvent envDeleteEvent = getEnvDeleteEvent(environmentDto);
            eventSender().sendEvent(envDeleteEvent, environmentDtoEvent.getHeaders());
        } catch (Exception e) {
            EnvDeleteFailedEvent failedEvent = EnvDeleteFailedEvent.builder()
                    .withEnvironmentID(environmentDto.getId())
                    .withException(e)
                    .withResourceCrn(environmentDto.getResourceCrn())
                    .withResourceName(environmentDto.getName())
                    .build();
            eventSender().sendEvent(failedEvent, environmentDtoEvent.getHeaders());
        }
    }

    @Override
    public String selector() {
        return DELETE_DATAHUB_CLUSTERS_EVENT.selector();
    }

    private PollingConfig getPollingConfig() {
        return PollingConfig.builder()
                .withStopPollingIfExceptionOccured(true)
                .withSleepTime(SLEEP_TIME)
                .withSleepTimeUnit(TimeUnit.SECONDS)
                .withTimeout(TIMEOUT)
                .withTimeoutTimeUnit(TimeUnit.HOURS)
                .build();
    }

    private EnvDeleteEvent getEnvDeleteEvent(EnvironmentDto environmentDto) {
        return EnvDeleteEvent.builder()
                .withResourceId(environmentDto.getResourceId())
                .withResourceName(environmentDto.getName())
                .withResourceCrn(environmentDto.getResourceCrn())
                .withSelector(START_DATALAKE_CLUSTERS_DELETE_EVENT.selector())
                .build();
    }
}
