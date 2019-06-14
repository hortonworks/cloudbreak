package com.sequenceiq.environment.environment.flow.creation.handler.freeipa;

import static com.sequenceiq.cloudbreak.polling.PollingResult.SUCCESS;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationHandlerSelectors.CREATE_FREEIPA_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.FINISH_ENV_CREATION_EVENT;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.polling.PollingService;
import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.configuration.SupportedPlatfroms;
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

    private String dummySshKey = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQC0Rfl2G2vDs6yc19RxCqReunFgpYj+ucyLobpTCBtfDwzIbJot2Fmife6M42mBtiTmAK6x8kc"
            + "UEeab6CB4MUzsqF7vGTFUjwWirG/XU5pYXFUBhi8xzey+KS9KVrQ+UuKJh/AN9iSQeMV+rgT1yF5+etVH+bK1/37QCKp3+mCqjFzPyQOrvkGZv4sYyRwX7BKBLleQmIVWpofpj"
            + "T7BfcCxH877RzC5YMIi65aBc82Dl6tH6OEiP7mzByU52yvH6JFuwZ/9fWj1vXCWJzxx2w0F1OU8Zwg8gNNzL+SVb9+xfBE7xBHMpYFg72hBWPh862Ce36F4NZd3MpWMSjMmpDPh"
            + "centos";

    private final EnvironmentService environmentService;

    private final FreeIpaV1Endpoint freeIpaV1Endpoint;

    private final SupportedPlatfroms supportedPlatfroms;

    private final Map<CloudPlatform, FreeIpaNetworkProvider> freeIpaNetworkProviderMapByCloudPlatform;

    private final PollingService<FreeIpaPollerObject> freeIpaPollingService;

    protected FreeipaCreationHandler(
            EventSender eventSender,
            EnvironmentService environmentService,
            FreeIpaV1Endpoint freeIpaV1Endpoint,
            SupportedPlatfroms supportedPlatfroms,
            Map<CloudPlatform, FreeIpaNetworkProvider> freeIpaNetworkProviderMapByCloudPlatform,
            PollingService<FreeIpaPollerObject> freeIpaPollingService) {
        super(eventSender);
        this.environmentService = environmentService;
        this.freeIpaV1Endpoint = freeIpaV1Endpoint;
        this.supportedPlatfroms = supportedPlatfroms;
        this.freeIpaNetworkProviderMapByCloudPlatform = freeIpaNetworkProviderMapByCloudPlatform;
        this.freeIpaPollingService = freeIpaPollingService;
    }

    @Override
    public void accept(Event<EnvironmentDto> environmentDtoEvent) {
        EnvironmentDto environmentDto = environmentDtoEvent.getData();
        Optional<Environment> environmentOptional = environmentService.findById(environmentDto.getId());

        try {
            if (environmentOptional.isPresent() && environmentOptional.get().isCreateFreeIpa()) {
                Environment environment = environmentOptional.get();
                if (supportedPlatfroms.supportedPlatformForFreeIpa(environment.getCloudPlatform())) {

                    updateStatus(environment);

                    PlacementRequest placementRequest = createPlacementRequest(environment);

                    CreateFreeIpaRequest createFreeIpaRequest = new CreateFreeIpaRequest();
                    createFreeIpaRequest.setEnvironmentCrn(environment.getResourceCrn());
                    createFreeIpaRequest.setName(environment.getName() + "-freeipa");
                    createFreeIpaRequest.setFreeIpa(createFreeIpaServerRequest());
                    createFreeIpaRequest.setPlacement(placementRequest);
                    createFreeIpaRequest.setAuthentication(createStackAuthenticationRequest());

                    FreeIpaNetworkProvider freeIpaNetworkProvider = freeIpaNetworkProviderMapByCloudPlatform
                            .get(CloudPlatform.valueOf(environment.getCloudPlatform()));
                    if (freeIpaNetworkProvider != null) {
                        createFreeIpaRequest.setNetwork(
                                freeIpaNetworkProvider.provider(environment));
                        placementRequest.setAvailabilityZone(
                                freeIpaNetworkProvider.availabilityZone(createFreeIpaRequest.getNetwork(), environment));
                    }

                    freeIpaV1Endpoint.create(createFreeIpaRequest);

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
            EnvCreationFailureEvent failureEvent = new EnvCreationFailureEvent(environmentDto.getId(), environmentDto.getName(), ex);
            eventSender().sendEvent(failureEvent, environmentDtoEvent.getHeaders());
        }
    }

    private EnvCreationEvent getNextStepObject(EnvironmentDto environmentDto) {
        return EnvCreationEvent.EnvCreationEventBuilder.anEnvCreationEvent()
                .withResourceId(environmentDto.getResourceId())
                .withSelector(FINISH_ENV_CREATION_EVENT.selector())
                .build();
    }

    private void updateStatus(Environment environment) {
        environment.setStatus(EnvironmentStatus.FREEIPA_CREATION_IN_PROGRESS);
        environmentService.save(environment);
    }

    private FreeIpaServerRequest createFreeIpaServerRequest() {
        FreeIpaServerRequest request = new FreeIpaServerRequest();
        request.setAdminPassword("Admin123!");
        request.setDomain("cdp.site");
        request.setHostname("ipaserver");
        return request;
    }

    private PlacementRequest createPlacementRequest(Environment env) {
        PlacementRequest request = new PlacementRequest();
        request.setRegion(env.getRegionSet().iterator().next().getName());
        return request;
    }

    private StackAuthenticationRequest createStackAuthenticationRequest() {
        StackAuthenticationRequest request = new StackAuthenticationRequest();
        request.setLoginUserName("cloudbreak");
        request.setPublicKey(dummySshKey);
        return request;
    }

    @Override
    public String selector() {
        return CREATE_FREEIPA_EVENT.selector();
    }

}
