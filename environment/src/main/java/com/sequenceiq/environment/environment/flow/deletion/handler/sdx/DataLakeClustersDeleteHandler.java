package com.sequenceiq.environment.environment.flow.deletion.handler.sdx;

import static com.sequenceiq.environment.environment.flow.deletion.event.EnvClustersDeleteStateSelectors.FINISH_ENV_CLUSTERS_DELETE_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_DATALAKE_CLUSTERS_EVENT;

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
public class DataLakeClustersDeleteHandler extends EventSenderAwareHandler<EnvironmentDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataLakeClustersDeleteHandler.class);

    private static final int SLEEP_TIME = 10;

    private static final int TIMEOUT = 1;

    private final EnvironmentService environmentService;

    private final SdxDeleteService sdxDeleteService;

    protected DataLakeClustersDeleteHandler(EventSender eventSender, EnvironmentService environmentService, SdxDeleteService sdxDeleteService) {
        super(eventSender);
        this.sdxDeleteService = sdxDeleteService;
        this.environmentService = environmentService;
    }

    @Override
    public void accept(Event<EnvironmentDto> environmentDtoEvent) {
        LOGGER.debug("Accepting DataLakeClustersDelete event");
        EnvironmentDto environmentDto = environmentDtoEvent.getData();
        try {
            PollingConfig pollingConfig = getPollingConfig();
            environmentService.findEnvironmentById(environmentDto.getId())
                    .ifPresent(environment -> sdxDeleteService.deleteSdxClustersForEnvironment(pollingConfig, environment));
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
        return DELETE_DATALAKE_CLUSTERS_EVENT.selector();
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
                .withSelector(FINISH_ENV_CLUSTERS_DELETE_EVENT.selector())
                .build();
    }
}
