package com.sequenceiq.environment.environment.flow.creation.handler.freeipa;

import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;
import static com.sequenceiq.cloudbreak.polling.PollingResult.SUCCESS;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationHandlerSelectors.CREATE_FREEIPA_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.FINISH_ENV_CREATION_EVENT;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.CREATE_IN_PROGRESS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.sequenceiq.common.api.backup.request.BackupRequest;
import com.sequenceiq.environment.environment.v1.converter.BackupConverter;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.polling.PollingService;
import com.sequenceiq.cloudbreak.util.CidrUtil;
import com.sequenceiq.common.api.telemetry.request.TelemetryRequest;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.configuration.SupportedPlatforms;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.AuthenticationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationAwsParametersDto;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationDto;
import com.sequenceiq.environment.environment.dto.SecurityAccessDto;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationEvent;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationFailureEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.environment.environment.v1.converter.TelemetryApiConverter;
import com.sequenceiq.environment.exception.FreeIpaOperationFailedException;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;
import com.sequenceiq.freeipa.api.v1.dns.DnsV1Endpoint;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsZoneForSubnetIdsRequest;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsZoneForSubnetsRequest;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsZoneNetwork;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.FreeIpaServerRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.attachchildenv.AttachChildEnvironmentRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.aws.AwsInstanceTemplateParameters;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.aws.AwsInstanceTemplateSpotParameters;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceTemplateRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.region.PlacementRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security.SecurityGroupRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security.SecurityRuleRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security.StackAuthenticationRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.CreateFreeIpaRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class FreeIpaCreationHandler extends EventSenderAwareHandler<EnvironmentDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaCreationHandler.class);

    private static final String MASTER_GROUP_NAME = "master0";

    private static final String TCP = "tcp";

    private static final List<String> DEFAULT_SECURITY_GROUP_PORTS = List.of("22");

    private final EnvironmentService environmentService;

    private final FreeIpaService freeIpaService;

    private final DnsV1Endpoint dnsV1Endpoint;

    private final SupportedPlatforms supportedPlatforms;

    private final Map<CloudPlatform, FreeIpaNetworkProvider> freeIpaNetworkProviderMapByCloudPlatform;

    private final PollingService<FreeIpaPollerObject> freeIpaPollingService;

    private final FreeIpaServerRequestProvider freeIpaServerRequestProvider;

    private final TelemetryApiConverter telemetryApiConverter;

    private final BackupConverter backupConverter;

    private final CloudPlatformConnectors connectors;

    private final EventBus eventBus;

    private Set<String> enabledChildPlatforms;

    public FreeIpaCreationHandler(
            EventSender eventSender,
            EnvironmentService environmentService,
            FreeIpaService freeIpaService,
            DnsV1Endpoint dnsV1Endpoint,
            SupportedPlatforms supportedPlatforms,
            Map<CloudPlatform, FreeIpaNetworkProvider> freeIpaNetworkProviderMapByCloudPlatform,
            PollingService<FreeIpaPollerObject> freeIpaPollingService,
            FreeIpaServerRequestProvider freeIpaServerRequestProvider,
            TelemetryApiConverter telemetryApiConverter,
            BackupConverter backupConverter,
            CloudPlatformConnectors connectors,
            EventBus eventBus, @Value("${environment.enabledChildPlatforms}") Set<String> enabledChildPlatforms) {
        super(eventSender);
        this.environmentService = environmentService;
        this.freeIpaService = freeIpaService;
        this.dnsV1Endpoint = dnsV1Endpoint;
        this.supportedPlatforms = supportedPlatforms;
        this.freeIpaNetworkProviderMapByCloudPlatform = freeIpaNetworkProviderMapByCloudPlatform;
        this.freeIpaPollingService = freeIpaPollingService;
        this.freeIpaServerRequestProvider = freeIpaServerRequestProvider;
        this.telemetryApiConverter = telemetryApiConverter;
        this.backupConverter = backupConverter;
        this.connectors = connectors;
        this.eventBus = eventBus;
        this.enabledChildPlatforms = enabledChildPlatforms;
    }

    @Override
    public void accept(Event<EnvironmentDto> environmentDtoEvent) {
        EnvironmentDto environmentDto = environmentDtoEvent.getData();
        Optional<Environment> environmentOptional = environmentService.findEnvironmentById(environmentDto.getId());
        try {
            if (environmentOptional.isPresent()) {
                Environment environment = environmentOptional.get();
                if (Objects.nonNull(environment.getParentEnvironment())) {
                    attachParentFreeIpa(environmentDto);
                } else if (environment.isCreateFreeIpa() && supportedPlatforms.supportedPlatformForFreeIpa(environment.getCloudPlatform())) {
                    createFreeIpa(environmentDtoEvent, environmentDto);
                }
            }
            eventSender().sendEvent(getNextStepObject(environmentDto), environmentDtoEvent.getHeaders());
        } catch (Exception ex) {
            LOGGER.error(String.format("Error occurred during creating FreeIpa for environment %s.", environmentDto), ex);
            EnvCreationFailureEvent failureEvent = new EnvCreationFailureEvent(environmentDto.getId(),
                    environmentDto.getName(), ex, environmentDto.getResourceCrn());
            eventBus.notify(failureEvent.selector(), new Event<>(environmentDtoEvent.getHeaders(), failureEvent));
        }
    }

    private void createFreeIpa(Event<EnvironmentDto> environmentDtoEvent, EnvironmentDto environmentDto) throws Exception {
        Optional<DescribeFreeIpaResponse> freeIpa = freeIpaService.describe(environmentDto.getResourceCrn());
        if (freeIpa.isEmpty()) {
            LOGGER.info("FreeIpa for environmentCrn '{}' was not found, creating a new one.", environmentDto.getResourceCrn());
            CreateFreeIpaRequest createFreeIpaRequest = createFreeIpaRequest(environmentDto);
            freeIpaService.create(createFreeIpaRequest);
            awaitFreeIpaCreation(environmentDtoEvent, environmentDto);
            AddDnsZoneForSubnetIdsRequest addDnsZoneForSubnetIdsRequest = addDnsZoneForSubnetIdsRequest(environmentDto);
            if (shouldSendSubnetIdsToFreeIpa(addDnsZoneForSubnetIdsRequest)) {
                dnsV1Endpoint.addDnsZoneForSubnetIds(addDnsZoneForSubnetIdsRequest);
            }
        } else {
            LOGGER.info("FreeIpa for environmentCrn '{}' already exists. Using this one.", environmentDto.getResourceCrn());
            if (CREATE_IN_PROGRESS == freeIpa.get().getStatus()) {
                awaitFreeIpaCreation(environmentDtoEvent, environmentDto);
            }
        }
    }

    private void attachParentFreeIpa(EnvironmentDto environmentDto) throws Exception {
        if (enabledChildPlatforms.stream().anyMatch(p -> p.equalsIgnoreCase(environmentDto.getCloudPlatform()))) {
            String parentEnvironmentCrn = environmentDto.getParentEnvironmentCrn();
            Optional<DescribeFreeIpaResponse> freeIpa = freeIpaService.describe(parentEnvironmentCrn);
            if (freeIpa.isPresent()) {
                LOGGER.info("Using FreeIpa of parent environment '{}' .", parentEnvironmentCrn);

                AttachChildEnvironmentRequest attachChildEnvironmentRequest = new AttachChildEnvironmentRequest();
                attachChildEnvironmentRequest.setChildEnvironmentCrn(environmentDto.getResourceCrn());
                attachChildEnvironmentRequest.setParentEnvironmentCrn(parentEnvironmentCrn);

                freeIpaService.attachChildEnvironment(attachChildEnvironmentRequest);

                AddDnsZoneForSubnetsRequest addDnsZoneForSubnetsRequest = new AddDnsZoneForSubnetsRequest();
                addDnsZoneForSubnetsRequest.setEnvironmentCrn(parentEnvironmentCrn);
                addDnsZoneForSubnetsRequest.setSubnets(Collections.singletonList(environmentDto.getNetwork().getNetworkCidr()));

                dnsV1Endpoint.addDnsZoneForSubnets(addDnsZoneForSubnetsRequest);
            }
        }
    }

    private boolean shouldSendSubnetIdsToFreeIpa(AddDnsZoneForSubnetIdsRequest addDnsZoneForSubnetIdsRequest) {
        AddDnsZoneNetwork addDnsZoneNetwork = addDnsZoneForSubnetIdsRequest.getAddDnsZoneNetwork();
        return StringUtils.isNotBlank(addDnsZoneNetwork.getNetworkId()) && !addDnsZoneNetwork.getSubnetIds().isEmpty();
    }

    private AddDnsZoneForSubnetIdsRequest addDnsZoneForSubnetIdsRequest(EnvironmentDto environmentDto) {
        AddDnsZoneForSubnetIdsRequest addDnsZoneForSubnetIdsRequest = new AddDnsZoneForSubnetIdsRequest();
        addDnsZoneForSubnetIdsRequest.setEnvironmentCrn(environmentDto.getResourceCrn());
        AddDnsZoneNetwork addDnsZoneNetwork = new AddDnsZoneNetwork();
        addDnsZoneNetwork.setNetworkId(environmentDto.getNetwork().getNetworkId());
        addDnsZoneNetwork.setSubnetIds(environmentDto.getNetwork().getSubnetIds());
        addDnsZoneForSubnetIdsRequest.setAddDnsZoneNetwork(addDnsZoneNetwork);
        return addDnsZoneForSubnetIdsRequest;
    }

    private CreateFreeIpaRequest createFreeIpaRequest(EnvironmentDto environment) {
        CreateFreeIpaRequest createFreeIpaRequest = new CreateFreeIpaRequest();
        createFreeIpaRequest.setEnvironmentCrn(environment.getResourceCrn());
        createFreeIpaRequest.setName(environment.getName() + "-freeipa");

        FreeIpaServerRequest freeIpaServerRequest = freeIpaServerRequestProvider.create(environment);
        createFreeIpaRequest.setFreeIpa(freeIpaServerRequest);

        setPlacementAndNetwork(environment, createFreeIpaRequest);
        setAuthentication(environment.getAuthentication(), createFreeIpaRequest);
        setTelemetry(environment, createFreeIpaRequest);
        setBackup(environment, createFreeIpaRequest);
        setTags(environment, createFreeIpaRequest);
        setImage(environment, createFreeIpaRequest);

        SecurityGroupRequest securityGroupRequest = null;
        if (environment.getSecurityAccess() != null) {
            securityGroupRequest = createSecurityGroupRequest(environment.getSecurityAccess());
        }
        createFreeIpaRequest.setInstanceGroups(createInstanceGroupRequests(securityGroupRequest, environment.getFreeIpaCreation()));

        setUseCcm(environment.getExperimentalFeatures().getTunnel(), createFreeIpaRequest);
        return createFreeIpaRequest;
    }

    private void setTags(EnvironmentDto environment, CreateFreeIpaRequest createFreeIpaRequest) {
        Map<String, String> userDefinedTags = environment.getTags().getUserDefinedTags();
        if (userDefinedTags == null) {
            userDefinedTags = new HashMap<>();
        }
        createFreeIpaRequest.setTags(userDefinedTags);
    }

    private void setTelemetry(EnvironmentDto environmentDto, CreateFreeIpaRequest createFreeIpaRequest) {
        TelemetryRequest telemetryRequest = telemetryApiConverter.convertToRequest(environmentDto.getTelemetry());
        createFreeIpaRequest.setTelemetry(telemetryRequest);
    }

    private void setBackup(EnvironmentDto environmentDto, CreateFreeIpaRequest createFreeIpaRequest) {
        BackupRequest backup = backupConverter.convertToRequest(environmentDto.getBackup());
        createFreeIpaRequest.setBackup(backup);
    }

    private void setPlacementAndNetwork(EnvironmentDto environment, CreateFreeIpaRequest createFreeIpaRequest) {
        PlacementRequest placementRequest = new PlacementRequest();
        String region = environment.getRegions().iterator().next().getName();
        Platform platform = platform(environment.getCloudPlatform().toUpperCase());
        placementRequest.setRegion(connectors.getDefault(platform).regionToDisplayName(region));
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

    private SecurityGroupRequest createSecurityGroupRequest(SecurityAccessDto securityAccess) {
        SecurityGroupRequest securityGroupRequest = new SecurityGroupRequest();
        if (!Strings.isNullOrEmpty(securityAccess.getCidr())) {
            securityGroupRequest.setSecurityRules(new ArrayList<>());
            for (String cidr : CidrUtil.cidrs(securityAccess.getCidr())) {
                SecurityRuleRequest securityRuleRequest = createSecurityRuleRequest(cidr);
                securityGroupRequest.getSecurityRules().add(securityRuleRequest);
            }
            securityGroupRequest.setSecurityGroupIds(new HashSet<>());
        } else if (!Strings.isNullOrEmpty(securityAccess.getDefaultSecurityGroupId())) {
            securityGroupRequest.setSecurityGroupIds(Set.of(securityAccess.getDefaultSecurityGroupId()));
            securityGroupRequest.setSecurityRules(new ArrayList<>());
        } else {
            securityGroupRequest.setSecurityRules(new ArrayList<>());
            securityGroupRequest.setSecurityGroupIds(new HashSet<>());
        }
        return securityGroupRequest;
    }

    private void setUseCcm(Tunnel tunnel, CreateFreeIpaRequest createFreeIpaRequest) {
        createFreeIpaRequest.setUseCcm(Tunnel.CCM == tunnel);
        createFreeIpaRequest.setTunnel(tunnel);
    }

    private List<InstanceGroupRequest> createInstanceGroupRequests(SecurityGroupRequest securityGroupRequest, FreeIpaCreationDto freeIpaCreation) {
        List<InstanceGroupRequest> instanceGroupRequests = new LinkedList<>();
        InstanceGroupRequest instanceGroupRequest = new InstanceGroupRequest();
        instanceGroupRequest.setName(MASTER_GROUP_NAME);
        instanceGroupRequest.setNodeCount(freeIpaCreation.getInstanceCountByGroup());
        instanceGroupRequest.setType(InstanceGroupType.MASTER);
        instanceGroupRequest.setSecurityGroup(securityGroupRequest);
        instanceGroupRequest.setInstanceTemplateRequest(createInstanceTemplate(freeIpaCreation));
        instanceGroupRequests.add(instanceGroupRequest);
        return instanceGroupRequests;
    }

    private InstanceTemplateRequest createInstanceTemplate(FreeIpaCreationDto freeIpaCreation) {
        InstanceTemplateRequest instanceTemplateRequest = new InstanceTemplateRequest();
        Optional.ofNullable(freeIpaCreation.getAws())
                .map(FreeIpaCreationAwsParametersDto::getSpot)
                .ifPresent(spotParametersDto -> {
                    AwsInstanceTemplateSpotParameters spot = new AwsInstanceTemplateSpotParameters();
                    spot.setPercentage(spotParametersDto.getPercentage());
                    spot.setMaxPrice(spotParametersDto.getMaxPrice());
                    AwsInstanceTemplateParameters aws = new AwsInstanceTemplateParameters();
                    aws.setSpot(spot);
                    instanceTemplateRequest.setAws(aws);
                });
        return instanceTemplateRequest;
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
                new FreeIpaCreationRetrievalTask(freeIpaService),
                new FreeIpaPollerObject(environment.getId(), environment.getResourceCrn()),
                FreeIpaCreationRetrievalTask.FREEIPA_RETRYING_INTERVAL,
                FreeIpaCreationRetrievalTask.FREEIPA_RETRYING_COUNT,
                FreeIpaCreationRetrievalTask.FREEIPA_FAILURE_COUNT);
        if (SUCCESS == pollWithTimeout.getKey()) {
            eventSender().sendEvent(getNextStepObject(environment), environmentDtoEvent.getHeaders());
        } else {
            LOGGER.info("FreeIPA creation polling has stopped due to the unsuccessful state/result: {}", pollWithTimeout.getKey());
            Optional.ofNullable(pollWithTimeout.getValue()).ifPresentOrElse(e -> {
                throw new FreeIpaOperationFailedException(e.getMessage());
            }, () -> {
                throw new FreeIpaOperationFailedException("Polling result was: " + pollWithTimeout.getKey());
            });
        }
    }

    private EnvCreationEvent getNextStepObject(EnvironmentDto environmentDto) {
        return EnvCreationEvent.builder()
                .withResourceId(environmentDto.getResourceId())
                .withResourceName(environmentDto.getName())
                .withResourceCrn(environmentDto.getResourceCrn())
                .withSelector(FINISH_ENV_CREATION_EVENT.selector())
                .build();
    }

    private void setImage(EnvironmentDto environment, CreateFreeIpaRequest createFreeIpaRequest) {
        if (environment.getFreeIpaCreation() != null) {
            String imageCatalog = environment.getFreeIpaCreation().getImageCatalog();
            String imageId = environment.getFreeIpaCreation().getImageId();
            if (!Strings.isNullOrEmpty(imageCatalog) && !Strings.isNullOrEmpty(imageId)) {
                LOGGER.info("FreeIPA creation with a pre-defined image catalog and image id: '{}', '{}'", imageCatalog, imageId);
                ImageSettingsRequest imageSettings = new ImageSettingsRequest();
                imageSettings.setCatalog(imageCatalog);
                imageSettings.setId(imageId);
                createFreeIpaRequest.setImage(imageSettings);
            } else {
                LOGGER.info("FreeIPA creation without pre-defined image settings. FreeIpa serivce is going to handle default settings.");
            }
        }
    }

    @Override
    public String selector() {
        return CREATE_FREEIPA_EVENT.selector();
    }
}
