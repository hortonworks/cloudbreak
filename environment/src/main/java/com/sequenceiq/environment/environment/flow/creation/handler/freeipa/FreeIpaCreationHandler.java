package com.sequenceiq.environment.environment.flow.creation.handler.freeipa;

import static com.sequenceiq.cloudbreak.polling.PollingResult.SUCCESS;
import static com.sequenceiq.cloudbreak.util.NullUtil.doIfNotNull;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationHandlerSelectors.CREATE_FREEIPA_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.FINISH_ENV_CREATION_EVENT;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.CREATE_IN_PROGRESS;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.NotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.common.exception.ClientErrorExceptionHandler;
import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.polling.PollingService;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.api.v1.environment.model.base.Tunnel;
import com.sequenceiq.environment.configuration.SupportedPlatforms;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.AuthenticationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.SecurityAccessDto;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationEvent;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationFailureEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.exception.FreeIpaOperationFailedException;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;
import com.sequenceiq.freeipa.api.v1.dns.DnsV1Endpoint;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsZoneForSubnetIdsRequest;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsZoneNetwork;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.FreeIpaServerRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.region.PlacementRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security.SecurityGroupRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security.SecurityRuleRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security.StackAuthenticationRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.CreateFreeIpaRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

import reactor.bus.Event;

@Component
public class FreeIpaCreationHandler extends EventSenderAwareHandler<EnvironmentDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaCreationHandler.class);

    private static final String MASTER_GROUP_NAME = "master";

    private static final int MASTER_NODE_COUNT = 1;

    private static final String TCP = "tcp";

    private static final List<String> DEFAULT_SECURITY_GROUP_PORTS = List.of("22", "443", "9443");

    private final EnvironmentService environmentService;

    private final FreeIpaV1Endpoint freeIpaV1Endpoint;

    private final DnsV1Endpoint dnsV1Endpoint;

    private final SupportedPlatforms supportedPlatforms;

    private final Map<CloudPlatform, FreeIpaNetworkProvider> freeIpaNetworkProviderMapByCloudPlatform;

    private final PollingService<FreeIpaPollerObject> freeIpaPollingService;

    @Inject
    private FreeIpaServerRequestProvider freeIpaServerRequestProvider;

    public FreeIpaCreationHandler(
            EventSender eventSender,
            EnvironmentService environmentService,
            FreeIpaV1Endpoint freeIpaV1Endpoint,
            DnsV1Endpoint dnsV1Endpoint,
            SupportedPlatforms supportedPlatforms,
            Map<CloudPlatform, FreeIpaNetworkProvider> freeIpaNetworkProviderMapByCloudPlatform,
            PollingService<FreeIpaPollerObject> freeIpaPollingService) {
        super(eventSender);
        this.environmentService = environmentService;
        this.freeIpaV1Endpoint = freeIpaV1Endpoint;
        this.dnsV1Endpoint = dnsV1Endpoint;
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
                    createFreeIpa(environmentDtoEvent, environmentDto, environment);
                }
            }
            eventSender().sendEvent(getNextStepObject(environmentDto), environmentDtoEvent.getHeaders());
        } catch (Exception ex) {
            EnvCreationFailureEvent failureEvent = new EnvCreationFailureEvent(environmentDto.getId(),
                    environmentDto.getName(), ex, environmentDto.getResourceCrn());
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
        } catch (NotFoundException nfe) {
            LOGGER.info("FreeIpa for environmentCrn '{}' was not found, creating a new one. Message: {}", environment.getResourceCrn(), nfe.getMessage());
            CreateFreeIpaRequest createFreeIpaRequest = createFreeIpaRequest(environmentDto);
            freeIpaV1Endpoint.create(createFreeIpaRequest);
            awaitFreeIpaCreation(environmentDtoEvent, environmentDto);
            AddDnsZoneForSubnetIdsRequest addDnsZoneForSubnetIdsRequest = addDnsZoneForSubnetIdsRequest(createFreeIpaRequest, environmentDto);
            if (shouldSendSubnetIdsToFreeIpa(addDnsZoneForSubnetIdsRequest)) {
                dnsV1Endpoint.addDnsZoneForSubnetIds(addDnsZoneForSubnetIdsRequest);
            }
        } catch (ClientErrorException e) {
            String errorMessage = ClientErrorExceptionHandler.getErrorMessage(e);
            LOGGER.info("Can not start FreeIPA provisioning: {}", errorMessage, e);
            throw new CloudbreakException("Can not start FreeIPA provisioning, client error happened on FreeIPA side: " + errorMessage, e);
        } catch (Exception e) {
            throw new CloudbreakException("Failed to create FreeIpa instance.", e);
        }
    }

    private boolean shouldSendSubnetIdsToFreeIpa(AddDnsZoneForSubnetIdsRequest addDnsZoneForSubnetIdsRequest) {
        AddDnsZoneNetwork addDnsZoneNetwork = addDnsZoneForSubnetIdsRequest.getAddDnsZoneNetwork();
        return StringUtils.isNotBlank(addDnsZoneNetwork.getNetworkId()) && !addDnsZoneNetwork.getSubnetIds().isEmpty();
    }

    private AddDnsZoneForSubnetIdsRequest addDnsZoneForSubnetIdsRequest(CreateFreeIpaRequest createFreeIpaRequest, EnvironmentDto environmentDto) {
        AddDnsZoneForSubnetIdsRequest addDnsZoneForSubnetIdsRequest = new AddDnsZoneForSubnetIdsRequest();
        addDnsZoneForSubnetIdsRequest.setEnvironmentCrn(environmentDto.getResourceCrn());
        AddDnsZoneNetwork addDnsZoneNetwork = new AddDnsZoneNetwork();
        addDnsZoneNetwork.setNetworkId(environmentDto.getNetwork().getNetworkId());
        addDnsZoneNetwork.setSubnetIds(environmentDto.getNetwork().getSubnetIds());
        addDnsZoneForSubnetIdsRequest.setAddDnsZoneNetwork(addDnsZoneNetwork);
        return addDnsZoneForSubnetIdsRequest;
    }

    private CreateFreeIpaRequest createFreeIpaRequest(EnvironmentDto environment) {
        CreateFreeIpaRequest createFreeIpaRequest = initFreeIpaRequest(environment);
        createFreeIpaRequest.setEnvironmentCrn(environment.getResourceCrn());

        FreeIpaServerRequest freeIpaServerRequest = freeIpaServerRequestProvider.create(environment);
        createFreeIpaRequest.setFreeIpa(freeIpaServerRequest);

        setPlacementAndNetwork(environment, createFreeIpaRequest);
        setAuthentication(environment.getAuthentication(), createFreeIpaRequest);
        doIfNotNull(environment.getSecurityAccess(), securityAccess -> setSecurityAccess(securityAccess, createFreeIpaRequest));
        setUseCcm(environment.getExperimentalFeatures().getTunnel(), createFreeIpaRequest);
        return createFreeIpaRequest;
    }

    private CreateFreeIpaRequest initFreeIpaRequest(EnvironmentDto environment) {
        CreateFreeIpaRequest createFreeIpaRequest = new CreateFreeIpaRequest();
        createFreeIpaRequest.setEnvironmentCrn(environment.getResourceCrn());
        createFreeIpaRequest.setName(environment.getName() + "-freeipa");
        return createFreeIpaRequest;
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

    private void setSecurityAccess(SecurityAccessDto securityAccess, CreateFreeIpaRequest createFreeIpaRequest) {
        SecurityGroupRequest securityGroupRequest = new SecurityGroupRequest();
        if (!Strings.isNullOrEmpty(securityAccess.getCidr())) {
            SecurityRuleRequest securityRuleRequest = createSecurityRuleRequest(securityAccess.getCidr());
            securityGroupRequest.setSecurityRules(List.of(securityRuleRequest));
            securityGroupRequest.setSecurityGroupIds(new HashSet<>());
        } else if (!Strings.isNullOrEmpty(securityAccess.getDefaultSecurityGroupId())) {
            securityGroupRequest.setSecurityGroupIds(Set.of(securityAccess.getDefaultSecurityGroupId()));
            securityGroupRequest.setSecurityRules(new ArrayList<>());
        } else {
            securityGroupRequest.setSecurityRules(new ArrayList<>());
            securityGroupRequest.setSecurityGroupIds(new HashSet<>());
        }
        InstanceGroupRequest instanceGroupRequest = createInstanceGroupRequest(securityGroupRequest);
        createFreeIpaRequest.setInstanceGroups(List.of(instanceGroupRequest));
    }

    private void setUseCcm(Tunnel tunnel, CreateFreeIpaRequest createFreeIpaRequest) {
        createFreeIpaRequest.setUseCcm(Tunnel.CCM == tunnel);
    }

    private InstanceGroupRequest createInstanceGroupRequest(SecurityGroupRequest securityGroupRequest) {
        InstanceGroupRequest instanceGroupRequest = new InstanceGroupRequest();
        instanceGroupRequest.setName(MASTER_GROUP_NAME);
        instanceGroupRequest.setNodeCount(MASTER_NODE_COUNT);
        instanceGroupRequest.setType(InstanceGroupType.MASTER);
        instanceGroupRequest.setSecurityGroup(securityGroupRequest);
        return instanceGroupRequest;
    }

    private SecurityRuleRequest createSecurityRuleRequest(String cidr) {
        SecurityRuleRequest securityRuleRequest = new SecurityRuleRequest();
        securityRuleRequest.setModifiable(false);
        securityRuleRequest.setPorts(DEFAULT_SECURITY_GROUP_PORTS);
        securityRuleRequest.setProtocol(TCP);
        securityRuleRequest.setSubnet(cidr);
        return securityRuleRequest;
    }

    private void awaitFreeIpaCreation(Event<EnvironmentDto> environmentDtoEvent, EnvironmentDto environment) {
        Pair<PollingResult, Exception> pollWithTimeout = freeIpaPollingService.pollWithTimeout(
                new FreeIpaCreationRetrievalTask(),
                new FreeIpaPollerObject(environment.getId(), environment.getResourceCrn(), freeIpaV1Endpoint),
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
                .withResourceName(environmentDto.getName())
                .withResourceCrn(environmentDto.getResourceCrn())
                .withSelector(FINISH_ENV_CREATION_EVENT.selector())
                .build();
    }

    @Override
    public String selector() {
        return CREATE_FREEIPA_EVENT.selector();
    }
}
