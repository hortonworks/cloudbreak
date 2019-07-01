package com.sequenceiq.environment.environment.flow.creation.handler.freeipa;

import static com.sequenceiq.cloudbreak.polling.PollingResult.SUCCESS;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationHandlerSelectors.CREATE_FREEIPA_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.FINISH_ENV_CREATION_EVENT;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.CREATE_IN_PROGRESS;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.ws.rs.NotFoundException;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.polling.PollingService;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.util.PasswordUtil;
import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.configuration.SupportedPlatforms;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.AuthenticationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationEvent;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationFailureEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.exception.FreeIpaOperationFailedException;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;
import com.sequenceiq.freeipa.api.v1.freeipa.dns.AddDnsZoneForSubnetIdsRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.FreeIpaServerRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.region.PlacementRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security.StackAuthenticationRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.CreateFreeIpaRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

import reactor.bus.Event;

@Component
public class FreeIpaCreationHandler extends EventSenderAwareHandler<EnvironmentDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaCreationHandler.class);

    private static final String FREEIPA_DOMAIN = "cloudera.site";

    private static final String FREEIPA_HOSTNAME = "ipaserver";

    private final EnvironmentService environmentService;

    private final FreeIpaV1Endpoint freeIpaV1Endpoint;

    private final SupportedPlatforms supportedPlatforms;

    private final Map<CloudPlatform, FreeIpaNetworkProvider> freeIpaNetworkProviderMapByCloudPlatform;

    private final PollingService<FreeIpaPollerObject> freeIpaPollingService;

    public FreeIpaCreationHandler(
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
                    createFreeIpa(environmentDtoEvent, environmentDto, environment);
                }
            }
            eventSender().sendEvent(getNextStepObject(environmentDto), environmentDtoEvent.getHeaders());
        } catch (Exception ex) {
            EnvCreationFailureEvent failureEvent = new EnvCreationFailureEvent(environmentDto.getId(), environmentDto.getName(), ex);
            eventSender().sendEvent(failureEvent, environmentDtoEvent.getHeaders());
        }
    }

    private void createFreeIpa(Event<EnvironmentDto> environmentDtoEvent, EnvironmentDto environmentDto, Environment environment) throws Exception {
        try {
            DescribeFreeIpaResponse freeIpa = freeIpaV1Endpoint.describe(environment.getResourceCrn());
            LOGGER.info("FreeIpa for environmentCrn '{}' already exists. Using this one.", environment.getResourceCrn());
            if (freeIpa.getStatus().equals(CREATE_IN_PROGRESS)) {
                awaitFreeIpaCreation(environmentDtoEvent, environmentDto);
            }
        } catch (Exception e) {
            if (e instanceof NotFoundException) {
                LOGGER.info("FreeIpa for environmentCrn '{}' was not found, creating a new one. Message: {}", environment.getResourceCrn(), e.getMessage());
                CreateFreeIpaRequest createFreeIpaRequest = createFreeIpaRequest(environmentDto);
                freeIpaV1Endpoint.create(createFreeIpaRequest);
                awaitFreeIpaCreation(environmentDtoEvent, environmentDto);
                AddDnsZoneForSubnetIdsRequest addDnsZoneForSubnetIdsRequest = addDnsZoneForSubnetIdsRequest(createFreeIpaRequest, environmentDto);
                if (shouldSendSubnetIdsToFreeIpa(addDnsZoneForSubnetIdsRequest)) {
                    freeIpaV1Endpoint.addDnsZoneForSubnetIds(addDnsZoneForSubnetIdsRequest);
                }
            } else {
                throw new CloudbreakException("Failed to create FreeIpa instance.", e);
            }
        }
    }

    private boolean shouldSendSubnetIdsToFreeIpa(AddDnsZoneForSubnetIdsRequest addDnsZoneForSubnetIdsRequest) {
        return !addDnsZoneForSubnetIdsRequest.getSubnetIds().isEmpty();
    }

    private AddDnsZoneForSubnetIdsRequest addDnsZoneForSubnetIdsRequest(CreateFreeIpaRequest createFreeIpaRequest, EnvironmentDto environmentDto) {
        AddDnsZoneForSubnetIdsRequest addDnsZoneForSubnetIdsRequest = new AddDnsZoneForSubnetIdsRequest();
        addDnsZoneForSubnetIdsRequest.setEnvironmentCrn(environmentDto.getResourceCrn());

        FreeIpaNetworkProvider freeIpaNetworkProvider = freeIpaNetworkProviderMapByCloudPlatform
                .get(CloudPlatform.valueOf(environmentDto.getCloudPlatform()));

        if (freeIpaNetworkProvider != null) {
            Set<String> subnetsForIpa = freeIpaNetworkProvider.getSubnets(createFreeIpaRequest.getNetwork());
            Set<String> allSubnets = environmentDto.getNetwork().getSubnetIds();

            allSubnets.removeAll(subnetsForIpa);

            addDnsZoneForSubnetIdsRequest.setSubnetIds(allSubnets);
        }
        return addDnsZoneForSubnetIdsRequest;
    }

    private CreateFreeIpaRequest createFreeIpaRequest(EnvironmentDto environment) {
        CreateFreeIpaRequest createFreeIpaRequest = initFreeIpaRequest(environment);
        createFreeIpaRequest.setEnvironmentCrn(environment.getResourceCrn());
        setFreeIpaServer(createFreeIpaRequest);
        setPlacementAndNetwork(environment, createFreeIpaRequest);
        setAuthentication(environment.getAuthentication(), createFreeIpaRequest);
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
        PlacementRequest placementRequest = new PlacementRequest();
        placementRequest.setRegion(environment.getRegionSet().iterator().next().getName());
        createFreeIpaRequest.setPlacement(placementRequest);

        FreeIpaNetworkProvider freeIpaNetworkProvider = freeIpaNetworkProviderMapByCloudPlatform
                .get(CloudPlatform.valueOf(environment.getCloudPlatform()));

        if (freeIpaNetworkProvider != null) {
            createFreeIpaRequest.setNetwork(
                    freeIpaNetworkProvider.provider(environment));
            placementRequest.setAvailabilityZone(
                    freeIpaNetworkProvider.availabilityZone(createFreeIpaRequest.getNetwork(), environment));
        }
    }

    private void setAuthentication(AuthenticationDto authentication, CreateFreeIpaRequest createFreeIpaRequest) {
        StackAuthenticationRequest stackAuthenticationRequest = new StackAuthenticationRequest();
        stackAuthenticationRequest.setLoginUserName(authentication.getLoginUserName());
        stackAuthenticationRequest.setPublicKey(authentication.getPublicKey());
        stackAuthenticationRequest.setPublicKeyId(authentication.getPublicKeyId());
        createFreeIpaRequest.setAuthentication(stackAuthenticationRequest);
    }

    private void awaitFreeIpaCreation(Event<EnvironmentDto> environmentDtoEvent, EnvironmentDto environment) {
        Pair<PollingResult, Exception> pollWithTimeout = freeIpaPollingService.pollWithTimeout(
                new FreeIpaCreationRetrievalTask(),
                new FreeIpaPollerObject(environment.getResourceCrn(), freeIpaV1Endpoint),
                FreeIpaCreationRetrievalTask.FREEIPA_RETRYING_INTERVAL,
                FreeIpaCreationRetrievalTask.FREEIPA_RETRYING_COUNT,
                FreeIpaCreationRetrievalTask.FREEIPA_FAILURE_COUNT);
        if (pollWithTimeout.getKey() == SUCCESS) {
            eventSender().sendEvent(getNextStepObject(environment), environmentDtoEvent.getHeaders());
        } else {
            throw new FreeIpaOperationFailedException(pollWithTimeout.getValue().getMessage());
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
