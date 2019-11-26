package com.sequenceiq.environment.environment.flow.deletion.handler;

import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_PUBLICKEY_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.START_NETWORK_DELETE_EVENT;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.PublicKeyConnector;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.publickey.PublicKeyUnregisterRequest;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteFailedEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;

@Component
public class PublicKeyDeleteHandler extends EventSenderAwareHandler<EnvironmentDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PublicKeyDeleteHandler.class);

    private final EnvironmentService environmentService;

    private final CloudPlatformConnectors cloudPlatformConnectors;

    private final CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    protected PublicKeyDeleteHandler(EventSender eventSender, EnvironmentService environmentService, CloudPlatformConnectors cloudPlatformConnectors,
            CredentialToCloudCredentialConverter credentialToCloudCredentialConverter) {

        super(eventSender);
        this.environmentService = environmentService;
        this.cloudPlatformConnectors = cloudPlatformConnectors;
        this.credentialToCloudCredentialConverter = credentialToCloudCredentialConverter;
    }

    @Override
    public String selector() {
        return DELETE_PUBLICKEY_EVENT.selector();
    }

    @Override
    public void accept(Event<EnvironmentDto> environmentDtoEvent) {
        LOGGER.debug("Accepting PublickeyDelete event");
        EnvironmentDto environmentDto = environmentDtoEvent.getData();
        try {
            environmentService.findEnvironmentById(environmentDto.getId()).ifPresent(this::deleteManagedKey);
            EnvDeleteEvent envDeleteEvent = getEnvDeleteEvent(environmentDto);
            eventSender().sendEvent(envDeleteEvent, environmentDtoEvent.getHeaders());
        } catch (Exception e) {
            EnvDeleteFailedEvent failedEvent = new EnvDeleteFailedEvent(environmentDto.getId(), environmentDto.getName(), e, environmentDto.getResourceCrn());
            eventSender().sendEvent(failedEvent, environmentDtoEvent.getHeaders());
        }
    }

    private void deleteManagedKey(Environment environment) {
        if (environment.getAuthentication().isManagedKey() && environment.getAuthentication().getPublicKeyId() != null) {
            LOGGER.debug("Environment {} has managed public key. Deleting.", environment.getName());
            deletePublicKey(environment);
        } else {
            LOGGER.debug("Environment {} had no managed public key", environment.getName());
        }
    }

    private void deletePublicKey(Environment environment) {
        try {
            PublicKeyConnector publicKeyConnector = getPublicKeyConnector(environment.getCloudPlatform())
                .orElseThrow(() -> new BadRequestException("No public key connector for cloud platform: " + environment.getCloudPlatform()));
            PublicKeyUnregisterRequest request = createPublicKeyUnregisterRequest(environment);
            publicKeyConnector.unregister(request);
        } catch (UnsupportedOperationException e) {
            LOGGER.info("Cloud platform {} does not support public key services", environment.getCloudPlatform());
        }
    }

    private Optional<PublicKeyConnector> getPublicKeyConnector(String cloudPlatform) {
        CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(Platform.platform(cloudPlatform), Variant.variant(cloudPlatform));
        return Optional.ofNullable(cloudPlatformConnectors.get(cloudPlatformVariant).publicKey());
    }

    private PublicKeyUnregisterRequest createPublicKeyUnregisterRequest(Environment environment) {
        CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(environment.getCredential());
        return PublicKeyUnregisterRequest.builder()
                .withCloudPlatform(environment.getCloudPlatform())
                .withCredential(cloudCredential)
                .withPublicKeyId(environment.getAuthentication().getPublicKeyId())
                .withRegion(environment.getLocation())
                .build();
    }

    private EnvDeleteEvent getEnvDeleteEvent(EnvironmentDto environmentDto) {
        return EnvDeleteEvent.builder()
                .withResourceId(environmentDto.getResourceId())
                .withResourceName(environmentDto.getName())
                .withResourceCrn(environmentDto.getResourceCrn())
                .withSelector(START_NETWORK_DELETE_EVENT.selector())
                .build();
    }
}
