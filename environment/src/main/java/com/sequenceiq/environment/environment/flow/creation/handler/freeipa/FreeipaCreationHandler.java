package com.sequenceiq.environment.environment.flow.creation.handler.freeipa;

import static com.sequenceiq.cloudbreak.polling.PollingResult.SUCCESS;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationHandlerSelectors.CREATE_FREEIPA_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.FINISH_ENV_CREATION_EVENT;

import java.security.KeyPair;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.certificate.PkiUtil;
import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.polling.PollingService;
import com.sequenceiq.cloudbreak.service.secret.model.StringToSecretResponseConverter;
import com.sequenceiq.cloudbreak.util.PasswordUtil;
import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.configuration.SupportedPlatfroms;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.FreeIpaServerRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.region.PlacementRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security.StackAuthenticationRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.CreateFreeIpaRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.credential.CredentialRequest;

import reactor.bus.Event;

@Component
public class FreeipaCreationHandler extends EventSenderAwareHandler<EnvironmentDto> {

    private final EnvironmentService environmentService;

    private final FreeIpaV1Endpoint freeIpaV1Endpoint;

    private final SupportedPlatfroms supportedPlatfroms;

    private final Map<CloudPlatform, FreeIpaNetworkProvider> freeIpaNetworkProviderMapByCloudPlatform;

    private final PollingService<FreeIpaPollerObject> freeIpaPollingService;

    private final StringToSecretResponseConverter secretConverter;

    protected FreeipaCreationHandler(
            EventSender eventSender,
            EnvironmentService environmentService,
            FreeIpaV1Endpoint freeIpaV1Endpoint,
            SupportedPlatfroms supportedPlatfroms,
            Map<CloudPlatform, FreeIpaNetworkProvider> freeIpaNetworkProviderMapByCloudPlatform,
            PollingService<FreeIpaPollerObject> freeIpaPollingService,
            StringToSecretResponseConverter secretConverter) {
        super(eventSender);
        this.environmentService = environmentService;
        this.freeIpaV1Endpoint = freeIpaV1Endpoint;
        this.supportedPlatfroms = supportedPlatfroms;
        this.freeIpaNetworkProviderMapByCloudPlatform = freeIpaNetworkProviderMapByCloudPlatform;
        this.freeIpaPollingService = freeIpaPollingService;
        this.secretConverter = secretConverter;
    }

    @Override
    public void accept(Event<EnvironmentDto> environmentDtoEvent) {
        EnvironmentDto environmentDto = environmentDtoEvent.getData();
        Optional<Environment> environmentOptional = environmentService.findById(environmentDto.getId());

        try {
            if (environmentOptional.isPresent()) {
                Environment environment = environmentOptional.get();
                if (supportedPlatfroms.supportedPlatformForFreeIpa(environment.getCloudPlatform())) {

                    CreateFreeIpaRequest createFreeIpaRequest = new CreateFreeIpaRequest();
                    createFreeIpaRequest.setEnvironmentCrn(environment.getResourceCrn());
                    createFreeIpaRequest.setName(environment.getName() + "-freeipa");

                    CredentialRequest credentialRequest = new CredentialRequest();
                    credentialRequest.setCloudPlatform(environment.getCloudPlatform());
                    credentialRequest.setName(environment.getCredential().getName());
                    credentialRequest.setSecret(secretConverter.convert(environment.getCredential().getAttributesSecret()));
                    createFreeIpaRequest.setCredential(credentialRequest);

                    FreeIpaServerRequest freeIpaServerRequest = new FreeIpaServerRequest();
                    freeIpaServerRequest.setAdminPassword(PasswordUtil.generatePassword());
                    freeIpaServerRequest.setDomain("cdp.site");
                    freeIpaServerRequest.setHostname("ipaserver");
                    createFreeIpaRequest.setFreeIpa(freeIpaServerRequest);

                    PlacementRequest placementRequest = new PlacementRequest();
                    placementRequest.setRegion(environment.getRegionSet().iterator().next().getName());
                    createFreeIpaRequest.setPlacement(placementRequest);

                    StackAuthenticationRequest stackAuthenticationRequest = new StackAuthenticationRequest();
                    stackAuthenticationRequest.setLoginUserName("cloudbreak");
                    KeyPair keyPair = PkiUtil.generateKeypair();
                    stackAuthenticationRequest.setPublicKey(PkiUtil.convertOpenSshPublicKey(keyPair.getPublic()));
                    createFreeIpaRequest.setAuthentication(stackAuthenticationRequest);


                    FreeIpaNetworkProvider freeIpaNetworkProvider = freeIpaNetworkProviderMapByCloudPlatform
                            .get(CloudPlatform.valueOf(environment.getCloudPlatform()));
                    if (freeIpaNetworkProvider != null) {
                        createFreeIpaRequest.setNetwork(
                                freeIpaNetworkProvider.provider(environment));
                        placementRequest.setAvailabilityZone(
                                freeIpaNetworkProvider.availabilityZone(createFreeIpaRequest.getNetwork(), environment));
                    }

                    freeIpaV1Endpoint.create(createFreeIpaRequest);

                    environment.setStatus(EnvironmentStatus.FREEIPA_CREATION_IN_PROGRESS);
                    environmentService.save(environment);

                    PollingResult result = freeIpaPollingService.pollWithTimeoutSingleFailure(
                            new FreeIpaCreationRetrievalTask(),
                            new FreeIpaPollerObject(environment.getResourceCrn(), freeIpaV1Endpoint),
                            FreeIpaCreationRetrievalTask.FREEIPA_RETRYING_INTERVAL,
                            FreeIpaCreationRetrievalTask.FREEIPA_RETRYING_COUNT);
                    if (result == SUCCESS) {
                        eventSender().sendEvent(getNextStepObject(environmentDto), environmentDtoEvent.getHeaders());
                    } else {
                        throw new FreeIpaOperationFailedException("Failed to prepare FreeIpa!");
                    }
                } else {
                    eventSender().sendEvent(getNextStepObject(environmentDto), environmentDtoEvent.getHeaders());
                }
            }
        } catch (Exception ex) {
            throw new FreeIpaOperationFailedException("Failed to prepare FreeIpa!", ex);
        }
    }

    private EnvCreationEvent getNextStepObject(EnvironmentDto environmentDto) {
        return EnvCreationEvent.EnvCreationEventBuilder.anEnvCreationEvent()
                .withResourceId(environmentDto.getResourceId())
                .withSelector(FINISH_ENV_CREATION_EVENT.selector())
                .build();
    }

    @Override
    public String selector() {
        return CREATE_FREEIPA_EVENT.selector();
    }
}
