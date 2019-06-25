package com.sequenceiq.environment.environment.flow.creation.handler.freeipa;

import static com.sequenceiq.cloudbreak.polling.PollingResult.SUCCESS;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationHandlerSelectors.CREATE_FREEIPA_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.FINISH_ENV_CREATION_EVENT;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.polling.PollingService;
import com.sequenceiq.cloudbreak.util.PasswordUtil;
import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.configuration.SupportedPlatforms;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationEvent;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationFailureEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.exception.FreeIpaOperationFailedException;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.FreeIpaServerRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.region.PlacementRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security.StackAuthenticationRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.CreateFreeIpaRequest;

import reactor.bus.Event;

@Component
public class FreeipaCreationHandler extends EventSenderAwareHandler<EnvironmentDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeipaCreationHandler.class);

    private static final String DUMMY_SSH_KEY = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQC0Rfl2G2vDs6yc19RxCqReunFgpYj+ucyLobpTCBtfDwzIbJot2Fmife6M42mBtiTmAK6x8kc"
            + "UEeab6CB4MUzsqF7vGTFUjwWirG/XU5pYXFUBhi8xzey+KS9KVrQ+UuKJh/AN9iSQeMV+rgT1yF5+etVH+bK1/37QCKp3+mCqjFzPyQOrvkGZv4sYyRwX7BKBLleQmIVWpofpj"
            + "T7BfcCxH877RzC5YMIi65aBc82Dl6tH6OEiP7mzByU52yvH6JFuwZ/9fWj1vXCWJzxx2w0F1OU8Zwg8gNNzL+SVb9+xfBE7xBHMpYFg72hBWPh862Ce36F4NZd3MpWMSjMmpDPh"
            + "centos";

    private static final String FREEIPA_DOMAIN = "cdp.site";

    private static final String FREEIPA_HOSTNAME = "ipaserver";

    private final EnvironmentService environmentService;

    private final FreeIpaV1Endpoint freeIpaV1Endpoint;

    private final SupportedPlatforms supportedPlatforms;

    private final Map<CloudPlatform, FreeIpaNetworkProvider> freeIpaNetworkProviderMapByCloudPlatform;

    private final PollingService<FreeIpaPollerObject> freeIpaPollingService;

    public FreeipaCreationHandler(
            EventSender eventSender,
            EnvironmentService environmentService,
            FreeIpaV1Endpoint freeIpaV1Endpoint,
            SupportedPlatforms supportedPlatforms,
            Map<CloudPlatform, FreeIpaNetworkProvider> freeIpaNetworkProviderMapByCloudPlatform,
            PollingService<FreeIpaPollerObject> freeIpaPollingService) {
        super(eventSender);
        this.environmentService = environmentService;
        this.freeIpaV1Endpoint = freeIpaV1Endpoint;
        this.supportedPlatforms = supportedPlatforms;
        this.freeIpaNetworkProviderMapByCloudPlatform = freeIpaNetworkProviderMapByCloudPlatform;
        this.freeIpaPollingService = freeIpaPollingService;
    }

    @Override
    public void accept(Event<EnvironmentDto> environmentDtoEvent) {
        EnvironmentDto environmentDto = environmentDtoEvent.getData();
        Optional<Environment> environmentOptional = environmentService.findEnvironmentById(environmentDto.getId());
        try {
            if (environmentOptional.isPresent() && environmentOptional.get().isCreateFreeIpa()) {
                Environment environment = environmentOptional.get();
                if (supportedPlatforms.supportedPlatformForFreeIpa(environment.getCloudPlatform())) {
                    environment.setStatus(EnvironmentStatus.FREEIPA_CREATION_IN_PROGRESS);
                    environmentService.save(environment);
                    CreateFreeIpaRequest createFreeIpaRequest = createFreeIpaRequest(environmentDto);
                    freeIpaV1Endpoint.create(createFreeIpaRequest);
                    awaitFreeIpaCreation(environmentDtoEvent, environmentDto);
                }
            }
            eventSender().sendEvent(getNextStepObject(environmentDto), environmentDtoEvent.getHeaders());
        } catch (Exception ex) {
            EnvCreationFailureEvent failureEvent = new EnvCreationFailureEvent(environmentDto.getId(), environmentDto.getName(), ex);
            eventSender().sendEvent(failureEvent, environmentDtoEvent.getHeaders());
        }
    }

    private CreateFreeIpaRequest createFreeIpaRequest(EnvironmentDto environment) {
        CreateFreeIpaRequest createFreeIpaRequest = initFreeIpaRequest(environment);
        createFreeIpaRequest.setEnvironmentCrn(environment.getResourceCrn());
        setFreeIpaServer(createFreeIpaRequest);
        setPlacementAndNetwork(environment, createFreeIpaRequest);
        setAuthentication(createFreeIpaRequest);
        return createFreeIpaRequest;
    }

    private CreateFreeIpaRequest initFreeIpaRequest(EnvironmentDto environment) {
        CreateFreeIpaRequest createFreeIpaRequest = new CreateFreeIpaRequest();
        createFreeIpaRequest.setEnvironmentCrn(environment.getResourceCrn());
        createFreeIpaRequest.setName(environment.getName() + "-freeipa");
        return createFreeIpaRequest;
    }

    private void setFreeIpaServer(CreateFreeIpaRequest createFreeIpaRequest) {
        FreeIpaServerRequest freeIpaServerRequest = new FreeIpaServerRequest();
        freeIpaServerRequest.setAdminPassword(PasswordUtil.generatePassword());
        freeIpaServerRequest.setDomain(FREEIPA_DOMAIN);
        freeIpaServerRequest.setHostname(FREEIPA_HOSTNAME);
        createFreeIpaRequest.setFreeIpa(freeIpaServerRequest);
    }

    private void setPlacementAndNetwork(EnvironmentDto environment, CreateFreeIpaRequest createFreeIpaRequest) {
        FreeIpaNetworkProvider freeIpaNetworkProvider = freeIpaNetworkProviderMapByCloudPlatform
                .get(CloudPlatform.valueOf(environment.getCloudPlatform()));
        PlacementRequest placementRequest = new PlacementRequest();
        placementRequest.setRegion(environment.getRegionSet().iterator().next().getName());
        createFreeIpaRequest.setPlacement(placementRequest);
        createFreeIpaRequest.setNetwork(
                freeIpaNetworkProvider.provider(environment));
        placementRequest.setAvailabilityZone(
                freeIpaNetworkProvider.availabilityZone(createFreeIpaRequest.getNetwork(), environment));
    }

    private void setAuthentication(CreateFreeIpaRequest createFreeIpaRequest) {
        StackAuthenticationRequest stackAuthenticationRequest = new StackAuthenticationRequest();
        stackAuthenticationRequest.setLoginUserName("cloudbreak");
        stackAuthenticationRequest.setPublicKey(DUMMY_SSH_KEY);
        createFreeIpaRequest.setAuthentication(stackAuthenticationRequest);
    }

    private void awaitFreeIpaCreation(Event<EnvironmentDto> environmentDtoEvent, EnvironmentDto environment) {
        PollingResult result = freeIpaPollingService.pollWithTimeoutSingleFailure(
                new FreeIpaCreationRetrievalTask(),
                new FreeIpaPollerObject(environment.getResourceCrn(), freeIpaV1Endpoint),
                FreeIpaCreationRetrievalTask.FREEIPA_RETRYING_INTERVAL,
                FreeIpaCreationRetrievalTask.FREEIPA_RETRYING_COUNT);
        if (result == SUCCESS) {
            eventSender().sendEvent(getNextStepObject(environment), environmentDtoEvent.getHeaders());
        } else {
            throw new FreeIpaOperationFailedException("Failed to prepare FreeIpa!");
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
