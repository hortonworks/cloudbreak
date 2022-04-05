package com.sequenceiq.environment.environment.flow.deletion.handler;

import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_IDBROKER_MAPPINGS_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.START_S3GUARD_TABLE_DELETE_EVENT;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.idbmms.GrpcIdbmmsClient;
import com.sequenceiq.cloudbreak.idbmms.exception.IdbmmsOperationErrorStatus;
import com.sequenceiq.cloudbreak.idbmms.exception.IdbmmsOperationException;
import com.sequenceiq.environment.api.v1.environment.model.base.IdBrokerMappingSource;
import com.sequenceiq.environment.environment.dto.EnvironmentDeletionDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;

@Component
public class IdBrokerMappingsDeleteHandler extends EventSenderAwareHandler<EnvironmentDeletionDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(IdBrokerMappingsDeleteHandler.class);

    private final HandlerExceptionProcessor exceptionProcessor;

    private final EnvironmentService environmentService;

    private final GrpcIdbmmsClient idbmmsClient;

    private final RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    protected IdBrokerMappingsDeleteHandler(EventSender eventSender, EnvironmentService environmentService, GrpcIdbmmsClient idbmmsClient,
            HandlerExceptionProcessor exceptionProcessor, RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        super(eventSender);
        this.idbmmsClient = idbmmsClient;
        this.environmentService = environmentService;
        this.exceptionProcessor = exceptionProcessor;
        this.regionAwareInternalCrnGeneratorFactory = regionAwareInternalCrnGeneratorFactory;
    }

    @Override
    public void accept(Event<EnvironmentDeletionDto> environmentDtoEvent) {
        EnvironmentDeletionDto environmentDeletionDto = environmentDtoEvent.getData();
        EnvironmentDto environmentDto = environmentDeletionDto.getEnvironmentDto();

        EnvDeleteEvent envDeleteEvent = EnvDeleteEvent.builder()
                .withResourceId(environmentDto.getResourceId())
                .withResourceName(environmentDto.getName())
                .withResourceCrn(environmentDto.getResourceCrn())
                .withForceDelete(environmentDeletionDto.isForceDelete())
                .withSelector(START_S3GUARD_TABLE_DELETE_EVENT.selector())
                .build();

        try {
            environmentService.findEnvironmentById(environmentDto.getId()).ifPresent(environment -> {
                String environmentCrn = environment.getResourceCrn();
                if (environment.getExperimentalFeaturesJson().getIdBrokerMappingSource() == IdBrokerMappingSource.IDBMMS) {
                    LOGGER.info("Deleting IDBroker mappings in IDBMMS associated with environment {}.", environmentCrn);
                    deleteIdBrokerMappings(environmentCrn);
                } else {
                    LOGGER.info("IDBMMS usage is disabled for environment {}. No need to delete IDBroker mappings.", environmentCrn);
                }
            });
            eventSender().sendEvent(envDeleteEvent, environmentDtoEvent.getHeaders());
        } catch (Exception e) {
            exceptionProcessor.handle(new HandlerFailureConjoiner(e, environmentDtoEvent, envDeleteEvent), LOGGER, eventSender(), selector());
        }
    }

    @Override
    public String selector() {
        return DELETE_IDBROKER_MAPPINGS_EVENT.selector();
    }

    private void deleteIdBrokerMappings(String environmentCrn) {
        try {
            // Must pass the internal actor here as this operation is internal-use only; requests with other actors will be always rejected.
            idbmmsClient.deleteMappings(regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(), environmentCrn, Optional.empty());
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
