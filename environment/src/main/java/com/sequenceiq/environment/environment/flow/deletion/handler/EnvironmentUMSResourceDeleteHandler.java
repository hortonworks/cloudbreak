package com.sequenceiq.environment.environment.flow.deletion.handler;

import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_UMS_RESOURCE_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.FINISH_ENV_DELETE_EVENT;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.security.InternalCrnBuilder;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;

@Component
public class EnvironmentUMSResourceDeleteHandler extends EventSenderAwareHandler<EnvironmentDto> {

    @VisibleForTesting
    static final String INTERNAL_ACTOR_CRN = new InternalCrnBuilder(Crn.Service.IAM).getInternalCrnForServiceAsString();

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentUMSResourceDeleteHandler.class);

    private final GrpcUmsClient umsClient;

    private final EnvironmentService environmentService;

    protected EnvironmentUMSResourceDeleteHandler(EventSender eventSender,
            EnvironmentService environmentService, GrpcUmsClient umsClient) {
        super(eventSender);
        this.environmentService = environmentService;
        this.umsClient = umsClient;
    }

    @Override
    public String selector() {
        return DELETE_UMS_RESOURCE_EVENT.selector();
    }

    @Override
    public void accept(Event<EnvironmentDto> environmentDtoEvent) {
        EnvironmentDto environmentDto = environmentDtoEvent.getData();
        String environmentCrn = null;
        try {
            environmentCrn = environmentDto.getResourceCrn();
            AtomicReference<String> resourceCrn = new AtomicReference<>(null);
            environmentService.findEnvironmentById(environmentDto.getId())
                    .ifPresent(environment -> {
                        resourceCrn.set(environment.getResourceCrn());
                    });
            if (StringUtils.isBlank(environmentCrn)) {
                environmentCrn = resourceCrn.get();
            }
            umsClient.notifyResourceDeleted(INTERNAL_ACTOR_CRN, environmentCrn, Optional.empty());
        } catch (Exception e) {
            LOGGER.warn("UMS delete event failed (this event is not critical)", e);
        }
        EnvDeleteEvent envDeleteEvent = EnvDeleteEvent.builder()
                .withResourceId(environmentDto.getResourceId())
                .withResourceName(environmentDto.getName())
                .withResourceCrn(environmentCrn)
                .withSelector(FINISH_ENV_DELETE_EVENT.selector())
                .build();
        eventSender().sendEvent(envDeleteEvent, environmentDtoEvent.getHeaders());
    }
}
