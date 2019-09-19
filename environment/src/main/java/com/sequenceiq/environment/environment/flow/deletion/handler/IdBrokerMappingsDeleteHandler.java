package com.sequenceiq.environment.environment.flow.deletion.handler;

import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_IDBROKER_MAPPINGS_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.FINISH_ENV_DELETE_EVENT;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.security.InternalCrnBuilder;
import com.sequenceiq.cloudbreak.idbmms.GrpcIdbmmsClient;
import com.sequenceiq.cloudbreak.idbmms.exception.IdbmmsOperationErrorStatus;
import com.sequenceiq.cloudbreak.idbmms.exception.IdbmmsOperationException;
import com.sequenceiq.environment.api.v1.environment.model.base.IdBrokerMappingSource;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteFailedEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;

@Component
public class IdBrokerMappingsDeleteHandler extends EventSenderAwareHandler<EnvironmentDto> {

    @VisibleForTesting
    static final String IAM_INTERNAL_ACTOR_CRN = new InternalCrnBuilder(Crn.Service.IAM).getInternalCrnForServiceAsString();

    private static final Logger LOGGER = LoggerFactory.getLogger(IdBrokerMappingsDeleteHandler.class);

    private final EnvironmentService environmentService;

    private final GrpcIdbmmsClient idbmmsClient;

    protected IdBrokerMappingsDeleteHandler(EventSender eventSender, EnvironmentService environmentService, GrpcIdbmmsClient idbmmsClient) {
        super(eventSender);
        this.environmentService = environmentService;
        this.idbmmsClient = idbmmsClient;
    }

    @Override
    public void accept(Event<EnvironmentDto> environmentDtoEvent) {
        EnvironmentDto environmentDto = environmentDtoEvent.getData();
        try {
            environmentService.findEnvironmentById(environmentDto.getId()).ifPresent(environment -> {
                String environmentCrn = environment.getResourceCrn();
                if (environment.getIdBrokerMappingSource() == IdBrokerMappingSource.IDBMMS) {
                    LOGGER.info("Deleting IDBroker mappings in IDBMMS associated with environment {}.", environmentCrn);
                    deleteIdBrokerMappings(environmentCrn);
                } else {
                    LOGGER.info("IDBMMS usage is disabled for environment {}. No need to delete IDBroker mappings.", environmentCrn);
                }
            });

            EnvDeleteEvent envDeleteEvent = EnvDeleteEvent.EnvDeleteEventBuilder.anEnvDeleteEvent()
                    .withResourceId(environmentDto.getResourceId())
                    .withResourceName(environmentDto.getName())
                    .withResourceCrn(environmentDto.getResourceCrn())
                    .withSelector(FINISH_ENV_DELETE_EVENT.selector())
                    .build();
            eventSender().sendEvent(envDeleteEvent, environmentDtoEvent.getHeaders());
        } catch (Exception e) {
            EnvDeleteFailedEvent failedEvent = new EnvDeleteFailedEvent(environmentDto.getId(), environmentDto.getName(), e, environmentDto.getResourceCrn());
            eventSender().sendEvent(failedEvent, environmentDtoEvent.getHeaders());
        }
    }

    @Override
    public String selector() {
        return DELETE_IDBROKER_MAPPINGS_EVENT.selector();
    }

    private void deleteIdBrokerMappings(String environmentCrn) {
        try {
            // Must pass the internal actor here as this operation is internal-use only; requests with other actors will be always rejected.
            idbmmsClient.deleteMappings(IAM_INTERNAL_ACTOR_CRN, environmentCrn, Optional.empty());
        } catch (IdbmmsOperationException e) {
            if (e.getErrorStatus() == IdbmmsOperationErrorStatus.NOT_FOUND) {
                // This is a non-fatal situation when deleting the environment.
                LOGGER.warn("No IDBroker mappings to delete in IDBMMS associated with environment {}.", environmentCrn);
            } else {
                throw e;
            }
        }
    }

}
