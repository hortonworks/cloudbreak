package com.sequenceiq.environment.environment.flow.deletion.handler.computecluster;

import static com.sequenceiq.environment.environment.flow.deletion.event.EnvClustersDeleteStateSelectors.FINISH_ENV_CLUSTERS_DELETE_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_COMPUTE_CLUSTERS_EVENT;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.dto.EnvironmentDeletionDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvClusterDeleteFailedEvent;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.service.externalizedcompute.ExternalizedComputeService;
import com.sequenceiq.environment.util.PollingConfig;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class ComputeClustersDeleteHandler extends ExceptionCatcherEventHandler<EnvironmentDeletionDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ComputeClustersDeleteHandler.class);

    private static final int SLEEP_TIME = 10;

    private static final int TIMEOUT = 90;

    private final ExternalizedComputeService externalizedComputeService;

    protected ComputeClustersDeleteHandler(ExternalizedComputeService externalizedComputeService) {
        this.externalizedComputeService = externalizedComputeService;
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<EnvironmentDeletionDto> event) {
        EnvironmentDeletionDto environmentDeletionDto = event.getData();
        EnvironmentDto environmentDto = environmentDeletionDto.getEnvironmentDto();
        return EnvClusterDeleteFailedEvent.builder()
                .withEnvironmentId(environmentDto.getId())
                .withException(e)
                .withResourceCrn(environmentDto.getResourceCrn())
                .withResourceName(environmentDto.getName())
                .build();
    }

    @Override
    protected Selectable doAccept(HandlerEvent<EnvironmentDeletionDto> event) {
        LOGGER.debug("Accepting Compute clusters delete event");
        EnvironmentDeletionDto environmentDeletionDto = event.getData();
        EnvironmentDto environmentDto = environmentDeletionDto.getEnvironmentDto();

        PollingConfig pollingConfig = getPollingConfig();
        externalizedComputeService.deleteComputeCluster(environmentDto.getResourceCrn(), pollingConfig);
        return getEnvDeleteEvent(environmentDeletionDto);
    }

    @Override
    public String selector() {
        return DELETE_COMPUTE_CLUSTERS_EVENT.selector();
    }

    private PollingConfig getPollingConfig() {
        return PollingConfig.builder()
                .withStopPollingIfExceptionOccured(true)
                .withSleepTime(SLEEP_TIME)
                .withSleepTimeUnit(TimeUnit.SECONDS)
                .withTimeout(TIMEOUT)
                .withTimeoutTimeUnit(TimeUnit.MINUTES)
                .build();
    }

    private EnvDeleteEvent getEnvDeleteEvent(EnvironmentDeletionDto environmentDeletionDto) {
        EnvironmentDto environmentDto = environmentDeletionDto.getEnvironmentDto();
        return EnvDeleteEvent.builder()
                .withResourceId(environmentDto.getResourceId())
                .withResourceName(environmentDto.getName())
                .withResourceCrn(environmentDto.getResourceCrn())
                .withForceDelete(environmentDeletionDto.isForceDelete())
                .withSelector(FINISH_ENV_CLUSTERS_DELETE_EVENT.selector())
                .build();
    }
}
