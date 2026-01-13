package com.sequenceiq.environment.environment.flow.creation.handler.freeipa;

import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;
import static com.sequenceiq.cloudbreak.util.SecurityGroupSeparator.getSecurityGroupIds;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationHandlerSelectors.CREATE_FREEIPA_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.START_COMPUTE_CLUSTER_CREATION_WAITING_EVENT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;
import com.sequenceiq.cloudbreak.polling.PollingService;
import com.sequenceiq.cloudbreak.util.CidrUtil;
import com.sequenceiq.common.api.backup.request.BackupRequest;
import com.sequenceiq.common.api.telemetry.request.TelemetryRequest;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.common.model.SeLinux;
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
import com.sequenceiq.environment.environment.v1.converter.BackupConverter;
import com.sequenceiq.environment.environment.v1.converter.TelemetryApiConverter;
import com.sequenceiq.environment.exception.FreeIpaOperationFailedException;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;
import com.sequenceiq.freeipa.api.v1.dns.DnsV1Endpoint;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsZoneForSubnetIdsRequest;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsZoneForSubnetsRequest;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsZoneNetwork;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.FreeIpaServerRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.attachchildenv.AttachChildEnvironmentRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupNetworkRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceTemplateRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.aws.AwsInstanceTemplateParameters;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.aws.AwsInstanceTemplateSpotParameters;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.region.PlacementRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security.SecurityGroupRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security.SecurityRuleRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security.StackAuthenticationRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.CreateFreeIpaRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.SecurityRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

@Component
public class FreeIpaCreationHandler extends EventSenderAwareHandler<EnvironmentDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaCreationHandler.class);

    private static final String MASTER_GROUP_NAME = "master";

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

    private final EntitlementService entitlementService;

    private final MultiAzValidator multiAzValidator;

    private final Set<String> enabledChildPlatforms;

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
            EventBus eventBus,
            EntitlementService entitlementService,
            MultiAzValidator multiAzValidator,
            @Value("${environment.enabledChildPlatforms}") Set<String> enabledChildPlatforms) {
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
        this.entitlementService = entitlementService;
        this.multiAzValidator = multiAzValidator;
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
                    createFreeIpa(environmentDto, environment.getFreeIpaPlatformVariant());
                } else {
                    boolean supported = supportedPlatforms.supportedPlatformForFreeIpa(environment.getCloudPlatform());
                    LOGGER.info("Freeipa won't create: parent: {}, create freeipa: {}, {} provider is supproted: {}", environment.getParentEnvironment(),
                            environment.isCreateFreeIpa(), environment.getCloudPlatform(), supported);
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

    private void createFreeIpa(EnvironmentDto environmentDto, String platformVariant) {
        Optional<DescribeFreeIpaResponse> freeIpa = freeIpaService.describe(environmentDto.getResourceCrn());
        if (freeIpa.isEmpty()) {
            LOGGER.info("FreeIpa for environmentCrn '{}' was not found, creating a new one.", environmentDto.getResourceCrn());
            awaitFreeIpaCreation(
                    environmentDto,
                    freeIpaService.create(createFreeIpaRequest(environmentDto, platformVariant)));
        } else {
            LOGGER.info("FreeIpa for environmentCrn '{}' already exists. Using this one.", environmentDto.getResourceCrn());
            FlowIdentifier flowIdentifier = ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> freeIpaService.getLastFlowId(freeIpa.get().getCrn())
            );
            awaitFreeIpaCreation(environmentDto, flowIdentifier);
        }
        AddDnsZoneForSubnetIdsRequest addDnsZoneForSubnetIdsRequest = createAddDnsZoneForSubnetIdsRequest(environmentDto);
        if (shouldSendSubnetIdsToFreeIpa(addDnsZoneForSubnetIdsRequest)) {
            dnsV1Endpoint.addDnsZoneForSubnetIds(addDnsZoneForSubnetIdsRequest);
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
                addDnsZoneForSubnetsRequest.setSubnets(getSubnetCidrs(environmentDto));

                dnsV1Endpoint.addDnsZoneForSubnets(addDnsZoneForSubnetsRequest);
            }
        }
    }

    private List<String> getSubnetCidrs(EnvironmentDto environmentDto) {
        if (environmentDto.getNetwork() != null) {
            if (environmentDto.getNetwork().getNetworkCidrs() != null) {
                return new ArrayList<>(environmentDto.getNetwork().getNetworkCidrs());
            } else if (!Strings.isNullOrEmpty(environmentDto.getNetwork().getNetworkCidr())) {
                return List.of(environmentDto.getNetwork().getNetworkCidr());
            }
        }
        return List.of();
    }

    private boolean shouldSendSubnetIdsToFreeIpa(AddDnsZoneForSubnetIdsRequest addDnsZoneForSubnetIdsRequest) {
        AddDnsZoneNetwork addDnsZoneNetwork = addDnsZoneForSubnetIdsRequest.getAddDnsZoneNetwork();
        return StringUtils.isNotBlank(addDnsZoneNetwork.getNetworkId()) && !addDnsZoneNetwork.getSubnetIds().isEmpty();
    }

    private AddDnsZoneForSubnetIdsRequest createAddDnsZoneForSubnetIdsRequest(EnvironmentDto environmentDto) {
        AddDnsZoneForSubnetIdsRequest addDnsZoneForSubnetIdsRequest = new AddDnsZoneForSubnetIdsRequest();
        addDnsZoneForSubnetIdsRequest.setEnvironmentCrn(environmentDto.getResourceCrn());
        AddDnsZoneNetwork addDnsZoneNetwork = new AddDnsZoneNetwork();
        addDnsZoneNetwork.setNetworkId(environmentDto.getNetwork().getNetworkId());
        addDnsZoneNetwork.setSubnetIds(environmentDto.getNetwork().getSubnetIds());
        addDnsZoneForSubnetIdsRequest.setAddDnsZoneNetwork(addDnsZoneNetwork);
        LOGGER.debug("Request for additional DNS zones: {}", addDnsZoneForSubnetIdsRequest);
        return addDnsZoneForSubnetIdsRequest;
    }

    private CreateFreeIpaRequest createFreeIpaRequest(EnvironmentDto environment, String platformVariant) {
        boolean multiAzRequired = environment.getFreeIpaCreation().isEnableMultiAz();

        CreateFreeIpaRequest createFreeIpaRequest = new CreateFreeIpaRequest();
        createFreeIpaRequest.setEnvironmentCrn(environment.getResourceCrn());
        createFreeIpaRequest.setName(environment.getName() + "-freeipa");
        createFreeIpaRequest.setEnableMultiAz(multiAzRequired);
        createFreeIpaRequest.setLoadBalancerType(environment.getFreeIpaCreation().getLoadBalancerType().toString());

        FreeIpaServerRequest freeIpaServerRequest = freeIpaServerRequestProvider.create(environment);
        createFreeIpaRequest.setFreeIpa(freeIpaServerRequest);

        setPlacementAndNetwork(environment, createFreeIpaRequest, multiAzRequired);
        setAuthentication(environment.getAuthentication(), createFreeIpaRequest);
        setTelemetry(environment, createFreeIpaRequest);
        setBackup(environment, createFreeIpaRequest);
        setTags(environment, createFreeIpaRequest);
        setImage(environment, createFreeIpaRequest);
        if (environment.getFreeIpaCreation().getArchitecture() != null) {
            createFreeIpaRequest.setArchitecture(environment.getFreeIpaCreation().getArchitecture().getName());
        }
        setSecurity(environment, createFreeIpaRequest);

        SecurityGroupRequest securityGroupRequest = null;
        if (environment.getSecurityAccess() != null) {
            securityGroupRequest = createSecurityGroupRequest(environment.getSecurityAccess());
        }
        createFreeIpaRequest.setInstanceGroups(createInstanceGroupRequests(createFreeIpaRequest, securityGroupRequest, environment, multiAzRequired));
        createFreeIpaRequest.setRecipes(environment.getFreeIpaCreation().getRecipes());
        setVariant(environment, createFreeIpaRequest, multiAzRequired, platformVariant);
        setUseCcm(environment.getExperimentalFeatures().getTunnel(), createFreeIpaRequest);
        return createFreeIpaRequest;
    }

    private void setSecurity(EnvironmentDto environment, CreateFreeIpaRequest createFreeIpaRequest) {
        SeLinux seLinux = environment.getFreeIpaCreation().getSeLinux();
        SecurityRequest securityRequest = new SecurityRequest();
        securityRequest.setSeLinux(seLinux == null ? SeLinux.PERMISSIVE.name() : seLinux.name());
        createFreeIpaRequest.setSecurity(securityRequest);
    }

    private void setVariant(EnvironmentDto environment, CreateFreeIpaRequest createFreeIpaRequest, boolean multiAzRequired, String platformVariant) {
        if (environment.getCredential().getGovCloud() != null && environment.getCredential().getGovCloud()
                && CloudPlatform.AWS.name().equals(environment.getCloudPlatform())) {
            createFreeIpaRequest.setVariant(AwsConstants.AwsVariant.AWS_NATIVE_GOV_VARIANT.variant().value());
        } else if ((multiAzRequired || entitlementService.enforceAwsNativeForSingleAzFreeipaEnabled(environment.getAccountId()))
                && CloudPlatform.AWS.name().equals(environment.getCloudPlatform())) {
            createFreeIpaRequest.setVariant(AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.variant().value());
        }
        if (!environment.getCredential().getGovCloud() && !multiAzRequired && platformVariant != null) {
            createFreeIpaRequest.setVariant(platformVariant);
        }
    }

    private void setTags(EnvironmentDto environment, CreateFreeIpaRequest createFreeIpaRequest) {
        Map<String, String> userDefinedTags = environment.getTags().getUserDefinedTags();
        if (userDefinedTags == null) {
            userDefinedTags = new HashMap<>();
        }
        createFreeIpaRequest.setTags(userDefinedTags);
    }

    private void setTelemetry(EnvironmentDto environmentDto, CreateFreeIpaRequest createFreeIpaRequest) {
        TelemetryRequest telemetryRequest = telemetryApiConverter.convertToRequest(environmentDto.getTelemetry(), environmentDto.getAccountId());
        createFreeIpaRequest.setTelemetry(telemetryRequest);
    }

    private void setBackup(EnvironmentDto environmentDto, CreateFreeIpaRequest createFreeIpaRequest) {
        BackupRequest backup = backupConverter.convertToRequest(environmentDto.getBackup());
        createFreeIpaRequest.setBackup(backup);
    }

    private void setPlacementAndNetwork(EnvironmentDto environment, CreateFreeIpaRequest createFreeIpaRequest, boolean multiAzRequired) {
        PlacementRequest placementRequest = new PlacementRequest();
        String region = environment.getRegions().iterator().next().getName();
        Platform platform = platform(environment.getCloudPlatform().toUpperCase(Locale.ROOT));
        placementRequest.setRegion(connectors.getDefault(platform).regionToDisplayName(region));
        createFreeIpaRequest.setPlacement(placementRequest);

        FreeIpaNetworkProvider freeIpaNetworkProvider = freeIpaNetworkProviderMapByCloudPlatform
                .get(CloudPlatform.valueOf(environment.getCloudPlatform()));

        if (freeIpaNetworkProvider != null) {
            createFreeIpaRequest.setNetwork(
                    freeIpaNetworkProvider.network(environment, multiAzRequired));
            if (!multiAzRequired) {
                placementRequest.setAvailabilityZone(
                        freeIpaNetworkProvider.availabilityZone(createFreeIpaRequest.getNetwork(), environment));
            }
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
            securityGroupRequest.setSecurityGroupIds(getSecurityGroupIds(securityAccess.getDefaultSecurityGroupId()));
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

    private List<InstanceGroupRequest> createInstanceGroupRequests(CreateFreeIpaRequest createFreeIpaRequest,
            SecurityGroupRequest securityGroupRequest,
            EnvironmentDto environment,
            boolean multiAzRequired) {
        List<InstanceGroupRequest> instanceGroupRequests = new LinkedList<>();
        FreeIpaCreationDto freeIpaCreation = environment.getFreeIpaCreation();
        InstanceGroupRequest instanceGroupRequest = new InstanceGroupRequest();
        instanceGroupRequest.setName(MASTER_GROUP_NAME);
        instanceGroupRequest.setNodeCount(freeIpaCreation.getInstanceCountByGroup());
        instanceGroupRequest.setType(InstanceGroupType.MASTER);
        instanceGroupRequest.setSecurityGroup(securityGroupRequest);
        instanceGroupRequest.setInstanceTemplateRequest(createInstanceTemplate(freeIpaCreation));
        if (multiAzRequired && multiAzValidator.suportedMultiAzForEnvironment(environment.getCloudPlatform())) {
            FreeIpaNetworkProvider freeIpaNetworkProvider = freeIpaNetworkProviderMapByCloudPlatform
                    .get(CloudPlatform.valueOf(environment.getCloudPlatform()));
            if (freeIpaNetworkProvider != null) {
                InstanceGroupNetworkRequest instanceGroupNetworkRequest = freeIpaNetworkProvider.networkByGroup(environment);
                instanceGroupRequest.setNetwork(instanceGroupNetworkRequest);
                createFreeIpaRequest.getPlacement().setAvailabilityZone(
                        freeIpaNetworkProvider.availabilityZone(instanceGroupNetworkRequest, environment));
            }
        }
        instanceGroupRequests.add(instanceGroupRequest);
        return instanceGroupRequests;
    }

    private InstanceTemplateRequest createInstanceTemplate(FreeIpaCreationDto freeIpaCreation) {
        InstanceTemplateRequest instanceTemplateRequest = new InstanceTemplateRequest();
        instanceTemplateRequest.setInstanceType(freeIpaCreation.getInstanceType());
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

    private void awaitFreeIpaCreation(EnvironmentDto environment, FlowIdentifier flowIdentifier) {
        ExtendedPollingResult pollWithTimeout = freeIpaPollingService.pollWithTimeout(
                new FreeIpaCreationRetrievalTask(freeIpaService),
                new FreeIpaPollerObject(environment.getId(), environment.getResourceCrn(), flowIdentifier),
                FreeIpaCreationRetrievalTask.FREEIPA_RETRYING_INTERVAL,
                FreeIpaCreationRetrievalTask.FREEIPA_RETRYING_COUNT,
                FreeIpaCreationRetrievalTask.FREEIPA_FAILURE_COUNT);
        if (!pollWithTimeout.isSuccess()) {
            LOGGER.info("FreeIPA creation polling has stopped due to the unsuccessful state/result: {}", pollWithTimeout.getPollingResult());
            Optional.ofNullable(pollWithTimeout.getException()).ifPresentOrElse(e -> {
                throw new FreeIpaOperationFailedException(e.getMessage());
            }, () -> {
                throw new FreeIpaOperationFailedException("Polling result was: " + pollWithTimeout.getPollingResult());
            });
        }
    }

    private EnvCreationEvent getNextStepObject(EnvironmentDto environmentDto) {
        return EnvCreationEvent.builder()
                .withResourceId(environmentDto.getResourceId())
                .withResourceName(environmentDto.getName())
                .withResourceCrn(environmentDto.getResourceCrn())
                .withSelector(START_COMPUTE_CLUSTER_CREATION_WAITING_EVENT.selector())
                .build();
    }

    private void setImage(EnvironmentDto environment, CreateFreeIpaRequest createFreeIpaRequest) {
        if (environment.getFreeIpaCreation() != null) {
            String imageCatalog = environment.getFreeIpaCreation().getImageCatalog();
            String imageId = environment.getFreeIpaCreation().getImageId();
            String imageOs = environment.getFreeIpaCreation().getImageOs();
            if (!Strings.isNullOrEmpty(imageId)) {
                LOGGER.info("FreeIPA creation with a pre-defined image catalog and image id: '{}', '{}'", imageCatalog, imageId);
                ImageSettingsRequest imageSettings = new ImageSettingsRequest();
                imageSettings.setCatalog(imageCatalog);
                imageSettings.setId(imageId);
                createFreeIpaRequest.setImage(imageSettings);
            } else if (!StringUtils.isAllBlank(imageOs, imageCatalog)) {
                LOGGER.info("FreeIPA creation with a pre-defined image catalog and OS type: '{}', '{}'", imageCatalog, imageOs);
                ImageSettingsRequest imageSettings = new ImageSettingsRequest();
                imageSettings.setCatalog(imageCatalog);
                imageSettings.setOs(imageOs);
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
