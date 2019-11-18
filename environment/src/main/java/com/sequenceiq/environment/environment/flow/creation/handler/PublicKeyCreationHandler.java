package com.sequenceiq.environment.environment.flow.creation.handler;

import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationHandlerSelectors.CREATE_PUBLICKEY_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.START_FREEIPA_CREATION_EVENT;

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
import com.sequenceiq.cloudbreak.cloud.model.publickey.PublicKeyRegisterRequest;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationEvent;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationFailureEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;

@Component
public class PublicKeyCreationHandler extends EventSenderAwareHandler<EnvironmentDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PublicKeyCreationHandler.class);

    private final EnvironmentService environmentService;

    private final CloudPlatformConnectors cloudPlatformConnectors;

    private final CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    protected PublicKeyCreationHandler(EventSender eventSender, EnvironmentService environmentService, CloudPlatformConnectors cloudPlatformConnectors,
            CredentialToCloudCredentialConverter credentialToCloudCredentialConverter) {

        super(eventSender);
        this.environmentService = environmentService;
        this.cloudPlatformConnectors = cloudPlatformConnectors;
        this.credentialToCloudCredentialConverter = credentialToCloudCredentialConverter;
    }

    @Override
    public String selector() {
        return CREATE_PUBLICKEY_EVENT.selector();
    }

    @Override
    public void accept(Event<EnvironmentDto> environmentDtoEvent) {
        LOGGER.debug("Accepting PublicKeyCreation event");
        EnvironmentDto environmentDto = environmentDtoEvent.getData();
        try {
            environmentService.findEnvironmentById(environmentDto.getId()).ifPresent(this::createManagedKey);
            EnvCreationEvent envCreationEvent = getEnvCreateEvent(environmentDto);
            eventSender().sendEvent(envCreationEvent, environmentDtoEvent.getHeaders());
        } catch (Exception e) {
            EnvCreationFailureEvent failedEvent =
                    new EnvCreationFailureEvent(environmentDto.getId(), environmentDto.getName(), e, environmentDto.getResourceCrn());
            eventSender().sendEvent(failedEvent, environmentDtoEvent.getHeaders());
        }
    }

    private void createManagedKey(Environment environment) {
        if (environment.getAuthentication().isManagedKey()) {
            LOGGER.debug("Environment {} requested managed public key. Creating.", environment.getName());
            String publicKeyId = String.format("%s-%s", environment.getName(), environment.getResourceCrn());
            createPublicKey(environment, publicKeyId);
            environment.getAuthentication().setPublicKeyId(publicKeyId);
            environmentService.save(environment);
        } else {
            LOGGER.debug("Environment {} requested no managed public key", environment.getName());
        }
    }

    private void createPublicKey(Environment environment, String publicKeyId) {
        try {
            PublicKeyConnector publicKeyConnector = getPublicKeyConnector(environment.getCloudPlatform())
                    .orElseThrow(() -> new BadRequestException("No network connector for cloud platform: " + environment.getCloudPlatform()));
            PublicKeyRegisterRequest request = createPublicKeyRegisterRequest(environment, publicKeyId);
            publicKeyConnector.register(request);
        } catch (UnsupportedOperationException e) {
            LOGGER.info("Cloud platform {} does not support public key services", environment.getCloudPlatform());
        }
    }

    private Optional<PublicKeyConnector> getPublicKeyConnector(String cloudPlatform) {
        CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(Platform.platform(cloudPlatform), Variant.variant(cloudPlatform));
        return Optional.ofNullable(cloudPlatformConnectors.get(cloudPlatformVariant).publicKey());
    }

    private PublicKeyRegisterRequest createPublicKeyRegisterRequest(Environment environment, String publicKeyId) {
        CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(environment.getCredential());
        return PublicKeyRegisterRequest.builder()
                .withCloudPlatform(environment.getCloudPlatform())
                .withCredential(cloudCredential)
                .withPublicKeyId(publicKeyId)
                .withPublicKey(environment.getAuthentication().getPublicKey())
                .withRegion(environment.getLocation())
                .build();
    }

    private EnvCreationEvent getEnvCreateEvent(EnvironmentDto environmentDto) {
        return EnvCreationEvent.builder()
                .withResourceId(environmentDto.getResourceId())
                .withResourceName(environmentDto.getName())
                .withResourceCrn(environmentDto.getResourceCrn())
                .withSelector(START_FREEIPA_CREATION_EVENT.selector())
                .build();
    }
}
