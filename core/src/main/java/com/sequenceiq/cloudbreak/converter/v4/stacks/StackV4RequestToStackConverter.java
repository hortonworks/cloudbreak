package com.sequenceiq.cloudbreak.converter.v4.stacks;

import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;
import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.SUBNET_ID;
import static com.sequenceiq.cloudbreak.util.Benchmark.measure;
import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;
import static com.sequenceiq.common.api.type.LoadBalancerSku.BASIC;
import static com.sequenceiq.common.api.type.LoadBalancerSku.STANDARD;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.network.aws.InstanceGroupAwsNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.network.azure.InstanceGroupAzureNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.network.gcp.InstanceGroupGcpNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.network.mock.InstanceGroupMockNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.network.yarn.InstanceGroupYarnNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.environment.placement.PlacementSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.network.InstanceGroupNetworkV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.TagsV4Request;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.StackInputs;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.cmtemplate.metering.MeteringServiceFieldResolver;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.converter.v4.environment.network.EnvironmentNetworkConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.authentication.StackAuthenticationV4RequestToStackAuthenticationConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.ClusterV4RequestToClusterConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.InstanceGroupV4RequestToHostGroupConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.InstanceGroupV4RequestToInstanceGroupConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.network.NetworkV4RequestToNetworkConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.stack.DnsResolverType;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.sdx.common.PlatformAwareSdxConnector;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.loadbalancer.LoadBalancerConfigService;
import com.sequenceiq.cloudbreak.service.stack.GatewaySecurityGroupDecorator;
import com.sequenceiq.cloudbreak.service.stack.TargetedUpscaleSupportService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.tag.ClusterTemplateApplicationTag;
import com.sequenceiq.cloudbreak.tag.CostTagging;
import com.sequenceiq.cloudbreak.tag.request.CDPTagMergeRequest;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringConfiguration;
import com.sequenceiq.cloudbreak.util.PasswordUtil;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

@Component
public class StackV4RequestToStackConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackV4RequestToStackConverter.class);

    @Value("${cb.ambari.username:cloudbreak}")
    private String cmAdminUserName;

    @Value("${cb.cm.mgmt.username:cmmgmt}")
    private String cmMgmtUsername;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private EnvironmentService environmentClientService;

    @Inject
    private ProviderParameterCalculator providerParameterCalculator;

    @Inject
    private MonitoringConfiguration monitoringConfiguration;

    @Inject
    private Clock clock;

    @Inject
    private Map<CloudPlatform, EnvironmentNetworkConverter> environmentNetworkConverterMap;

    @Inject
    private TelemetryConverter telemetryConverter;

    @Inject
    private GatewaySecurityGroupDecorator gatewaySecurityGroupDecorator;

    @Inject
    private CostTagging costTagging;

    @Value("${cb.platform.default.regions:}")
    private String defaultRegions;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Inject
    private MeteringServiceFieldResolver meteringServiceFieldResolver;

    @Inject
    private LoadBalancerConfigService loadBalancerConfigService;

    @Inject
    private PlatformAwareSdxConnector platformAwareSdxConnector;

    @Inject
    private ClusterV4RequestToClusterConverter clusterV4RequestToClusterConverter;

    @Inject
    private NetworkV4RequestToNetworkConverter networkV4RequestToNetworkConverter;

    @Inject
    private InstanceGroupV4RequestToHostGroupConverter instanceGroupV4RequestToHostGroupConverter;

    @Inject
    private InstanceGroupV4RequestToInstanceGroupConverter instanceGroupV4RequestToInstanceGroupConverter;

    @Inject
    private StackAuthenticationV4RequestToStackAuthenticationConverter stackAuthenticationV4RequestToStackAuthenticationConverter;

    @Inject
    private TargetedUpscaleSupportService targetedUpscaleSupportService;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private DatabaseRequestToDatabaseConverter databaseRequestToDatabaseConverter;

    public Stack convert(StackV4Request source) {
        return convert(null, source);
    }

    public Stack convert(DetailedEnvironmentResponse environment, StackV4Request source) {
        Workspace workspace = workspaceService.getForCurrentUser();

        Stack stack = new Stack();
        stack.setEnvironmentCrn(source.getEnvironmentCrn());
        if (!isEmpty(source.getEnvironmentCrn())) {
            environment = measure(() -> environmentClientService.getByCrn(source.getEnvironmentCrn()),
                    LOGGER, "Environment responded in {} ms for stack {}", source.getName());
        }
        if (isTemplate(source)) {
            updateCustomDomainOrKerberos(source, stack);
            updateCloudPlatformAndRelatedFields(source, stack, environment);
            convertAsStackTemplate(source, stack, environment);
            setNetworkAsTemplate(source, stack);
        } else {
            convertAsStack(source, stack);
            updateCloudPlatformAndRelatedFields(source, stack, environment);
            setNetworkIfApplicable(source, stack, environment);
            setInstanceGroupNetworkIfApplicable(source, stack, environment);
            stack.getComponents().add(getTelemetryComponent(stack, source));
        }
        Map<String, Object> asMap = providerParameterCalculator.get(source).asMap();
        if (asMap != null) {
            Map<String, String> parameter = new HashMap<>();
            asMap.forEach((key, value) -> parameter.put(key, value.toString()));
            stack.setParameters(parameter);
        }
        setTimeToLive(source, stack);
        stack.setWorkspace(workspace);
        stack.setDisplayName(source.getName());
        if (source.getSharedService() != null) {
            platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(environment.getCrn()).ifPresent(sdx -> stack.setDatalakeCrn(sdx.crn()));
        }
        stack.setStackAuthentication(stackAuthenticationV4RequestToStackAuthenticationConverter
                .convert(source.getAuthentication()));
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.PROVISION_REQUESTED));
        stack.setCreated(clock.getCurrentTimeMillis());
        stack.setInstanceGroups(convertInstanceGroups(source, stack));
        measure(() -> updateCluster(source, stack),
                LOGGER, "Converted cluster and updated the stack in {} ms for stack {}", source.getName());
        stack.setGatewayPort(source.getGatewayPort());
        stack.setUuid(UUID.randomUUID().toString());
        stack.setType(source.getType());
        stack.setInputs(Json.silent(new StackInputs(source.getInputs(), new HashMap<>(), new HashMap<>())));
        if (source.getImage() != null) {
            stack.getComponents().add(getImageComponent(source, stack));
        }
        if (!isTemplate(source) && environment != null) {
            gatewaySecurityGroupDecorator.extendGatewaySecurityGroupWithDefaultGatewayCidrs(stack, environment.getTunnel());
        }
        convertDatabase(source, stack);
        stack.setDomainDnsResolver(targetedUpscaleSupportService.isUnboundEliminationSupported(Crn.safeFromString(source.getEnvironmentCrn()).getAccountId()) ?
                DnsResolverType.FREEIPA_FOR_ENV : DnsResolverType.LOCAL_UNBOUND);
        determineServiceTypeTag(stack, source.getTags());
        determineServiceFeatureTag(stack, source.getTags());

        createStandardLoadBalancers(environment, source, stack);
        stack.setJavaVersion(source.getJavaVersion());
        stack.setArchitecture(Architecture.fromStringWithValidation(source.getArchitecture()));
        return stack;
    }

    private void createStandardLoadBalancers(DetailedEnvironmentResponse environment, StackV4Request source, Stack stack) {
        Set<LoadBalancer> loadBalancers = loadBalancerConfigService.createLoadBalancers(stack, environment, source);
        loadBalancers.forEach(lb -> {
            if (BASIC.equals(lb.getSku())) {
                LOGGER.debug("Overriding BASIC Load Balancer SKU to STANDARD as BASIC is no longer supported by Azure");
                lb.setSku(STANDARD);
            }
        });
        stack.setLoadBalancers(loadBalancers);
    }

    private void convertDatabase(StackV4Request source, Stack stack) {
        stack.setDatabase(databaseRequestToDatabaseConverter.convert(source.getCloudPlatform(), source.getExternalDatabase(),
                source.isDisableDbSslEnforcement()));
    }

    private void setTimeToLive(StackV4Request source, Stack stack) {
        if (source.getTimeToLive() != null) {
            stack.getParameters().put(PlatformParametersConsts.TTL_MILLIS, source.getTimeToLive().toString());
        }
    }

    private boolean isTemplate(StackV4Request source) {
        return source.getType() == StackType.TEMPLATE;
    }

    private void convertAsStack(StackV4Request source, Stack stack) {
        validateStackAuthentication(source);
        stack.setName(source.getName());
        stack.setAvailabilityZone(getAvailabilityZone(Optional.ofNullable(source.getPlacement())));
        stack.setOrchestrator(getOrchestrator());
        stack.setMultiAz(source.isEnableMultiAz());
        updateCustomDomainOrKerberos(source, stack);
    }

    private void updateCustomDomainOrKerberos(StackV4Request source, Stack stack) {
        if (source.getCustomDomain() != null) {
            stack.setCustomDomain(source.getCustomDomain().getDomainName());
            stack.setCustomHostname(source.getCustomDomain().getHostname());
            stack.setClusterNameAsSubdomain(source.getCustomDomain().isClusterNameAsSubdomain());
            stack.setHostgroupNameAsHostname(source.getCustomDomain().isHostgroupNameAsHostname());
        }
        // Host names shall be prefixed with stack name if not configured otherwise
        if (isEmpty(stack.getCustomHostname())) {
            stack.setCustomHostname(stack.getName());
        }
    }

    private com.sequenceiq.cloudbreak.domain.stack.Component getTelemetryComponent(Stack stack, StackV4Request source) {
        Telemetry telemetry = telemetryConverter.convert(source.getTelemetry(), source.getType(), ThreadBasedUserCrnProvider.getAccountId());
        try {
            return new com.sequenceiq.cloudbreak.domain.stack.Component(ComponentType.TELEMETRY, ComponentType.TELEMETRY.name(), Json.silent(telemetry), stack);
        } catch (Exception e) {
            LOGGER.debug("Exception during reading telemetry settings.", e);
            throw new BadRequestException("Failed to convert dynamic telemetry settingss.");
        }
    }

    private void updateCloudPlatformAndRelatedFields(StackV4Request source, Stack stack, DetailedEnvironmentResponse environment) {
        String cloudPlatform = determineCloudPlatform(source, environment);
        source.setCloudPlatform(CloudPlatform.valueOf(cloudPlatform));
        stack.setRegion(getIfNotNull(source.getPlacement(), s -> getRegion(source, cloudPlatform)));
        stack.setCloudPlatform(cloudPlatform);
        stack.setTags(getTags(source, environment));
        stack.setPlatformVariant(source.getVariant());
    }

    private void convertAsStackTemplate(StackV4Request source, Stack stack, DetailedEnvironmentResponse environment) {
        if (environment != null) {
            updateCloudPlatformAndRelatedFields(source, stack, environment);
            stack.setAvailabilityZone(getAvailabilityZone(Optional.ofNullable(source.getPlacement())));
        }
        stack.setType(StackType.TEMPLATE);
        stack.setName(UUID.randomUUID().toString());
    }

    private Orchestrator getOrchestrator() {
        Orchestrator orchestrator = new Orchestrator();
        orchestrator.setType("SALT");
        return orchestrator;
    }

    private String getAvailabilityZone(Optional<PlacementSettingsV4Request> placement) {
        return placement.map(PlacementSettingsV4Request::getAvailabilityZone).orElse(null);
    }

    private String getRegion(StackV4Request source, String cloudPlatform) {
        if (isEmpty(source.getPlacement().getRegion())) {
            Map<Platform, Region> regions = Maps.newHashMap();
            if (isNotEmpty(defaultRegions)) {
                for (String entry : defaultRegions.split(",")) {
                    String[] keyValue = entry.split(":");
                    regions.put(platform(keyValue[0]), Region.region(keyValue[1]));
                }
                Region platformRegion = regions.get(platform(cloudPlatform));
                if (platformRegion == null || isEmpty(platformRegion.value())) {
                    throw new BadRequestException(format("No default region specified for: %s. Region cannot be empty.", cloudPlatform));
                }
                return platformRegion.value();
            } else {
                throw new BadRequestException("No default region is specified. Region cannot be empty.");
            }
        }
        return source.getPlacement().getRegion();
    }

    private String determineCloudPlatform(StackV4Request source, DetailedEnvironmentResponse environmentResponse) {
        if (source.getCloudPlatform() != null) {
            return source.getCloudPlatform().name();
        }
        return environmentResponse == null ? "UNKNOWN" : environmentResponse.getCloudPlatform();
    }

    private Json getTags(StackV4Request source, DetailedEnvironmentResponse environment) {
        try {
            TagsV4Request tags = source.getTags();
            if (tags == null) {
                Map<String, String> userDefined = environment == null || environment.getTags() == null ? new HashMap<>() :
                        environment.getTags().getUserDefined();
                return new Json(new StackTags(userDefined, new HashMap<>(), new HashMap<>()));
            }

            Map<String, String> userDefined = new HashMap<>();
            if (environment != null && environment.getTags() != null && environment.getTags().getUserDefined() != null &&
                    !environment.getTags().getUserDefined().isEmpty()) {
                userDefined = environment.getTags().getUserDefined();
            }

            CDPTagMergeRequest request = CDPTagMergeRequest.Builder
                    .builder()
                    .withPlatform(source.getCloudPlatform().name())
                    .withRequestTags(tags.getUserDefined() != null ? tags.getUserDefined() : Maps.newHashMap())
                    .withEnvironmentTags(userDefined)
                    .build();
            return new Json(new StackTags(costTagging.mergeTags(request), tags.getApplication(), new HashMap<>()));
        } catch (Exception e) {
            throw new BadRequestException("Failed to convert dynamic tags. " + e.getMessage(), e);
        }
    }

    private void validateStackAuthentication(StackV4Request source) {
        if (isEmpty(source.getAuthentication().getPublicKey())
                && isEmpty(source.getAuthentication().getPublicKeyId())) {
            throw new BadRequestException("You should define the publickey or publickeyid!");
        } else if (source.getAuthentication().getLoginUserName() != null) {
            throw new BadRequestException("You can not modify the default user!");
        }
    }

    private Set<InstanceGroup> convertInstanceGroups(StackV4Request source, Stack stack) {
        if (source.getInstanceGroups() == null) {
            return null;
        }
        Set<InstanceGroup> convertedSet = new HashSet<>();
        source.getInstanceGroups().stream()
                .map(ig -> {
                    ig.setCloudPlatform(source.getCloudPlatform());
                    return instanceGroupV4RequestToInstanceGroupConverter.convert(ig, stack.getPlatformVariant());
                })
                .forEach(ig -> {
                    ig.setStack(stack);
                    convertedSet.add(ig);
                });
        return convertedSet;
    }

    private void updateCluster(StackV4Request source, Stack stack) {
        if (source.getCluster() != null) {
            source.getCluster().setName(stack.getName());
            Cluster cluster = clusterV4RequestToClusterConverter.convert(source.getCluster());
            fillCredentialValues(source, cluster);
            Set<HostGroup> hostGroups = source.getInstanceGroups().stream()
                    .map(ig -> {
                        HostGroup hostGroup = instanceGroupV4RequestToHostGroupConverter.convert(ig);
                        hostGroup.setCluster(cluster);
                        return hostGroup;
                    })
                    .collect(Collectors.toSet());
            cluster.setHostGroups(hostGroups);
            cluster.setAutoTlsEnabled(true);
            cluster.setDbSslEnabled(!source.isDisableDbSslEnforcement());
            stack.setCluster(cluster);
        }
    }

    private void fillCredentialValues(StackV4Request source, Cluster cluster) {
        if (source.getType() != StackType.TEMPLATE) {
            cluster.setUserName(source.getCluster().getUserName());
            cluster.setPassword(source.getCluster().getPassword());
            cluster.setCloudbreakClusterManagerUser(cmAdminUserName);
            cluster.setCloudbreakClusterManagerPassword(PasswordUtil.generatePassword());
            if (monitoringConfiguration.getClouderaManagerExporter() != null) {
                cluster.setCloudbreakClusterManagerMonitoringUser(monitoringConfiguration.getClouderaManagerExporter().getUser());
                cluster.setCloudbreakClusterManagerMonitoringPassword(PasswordUtil.generatePassword());
            }
            cluster.setCdpNodeStatusMonitorPassword(PasswordUtil.generatePassword());
            cluster.setDpClusterManagerUser(cmMgmtUsername);
            cluster.setDpClusterManagerPassword(PasswordUtil.generatePassword());
            cluster.setKeyStorePwd(PasswordUtil.generatePassword());
            cluster.setTrustStorePwd(PasswordUtil.generatePassword());
        }
    }

    private com.sequenceiq.cloudbreak.domain.stack.Component getImageComponent(StackV4Request source, Stack stack) {
        ImageSettingsV4Request imageSettings = source.getImage();
        Image image = new Image(null,
                null,
                imageSettings.getOs(),
                null,
                source.getArchitecture(),
                null,
                imageSettings.getCatalog(),
                imageSettings.getId(),
                null,
                null,
                null,
                null);
        return new com.sequenceiq.cloudbreak.domain.stack.Component(ComponentType.IMAGE, ComponentType.IMAGE.name(), Json.silent(image), stack);
    }

    private void setNetworkAsTemplate(StackV4Request source, Stack stack) {
        if (source.getNetwork() != null) {
            source.getNetwork().setCloudPlatform(source.getCloudPlatform());
            stack.setNetwork(networkV4RequestToNetworkConverter.convert(source.getNetwork()));
        }
    }

    private void setInstanceGroupNetworkIfApplicable(StackV4Request source, Stack stack, DetailedEnvironmentResponse environment) {
        String subnetId = getStackSubnetIdIfExists(stack);
        List<InstanceGroupV4Request> instanceGroups = source.getInstanceGroups();
        for (InstanceGroupV4Request instanceGroup : instanceGroups) {
            InstanceGroupNetworkV4Request instanceGroupNetworkV4Request = new InstanceGroupNetworkV4Request();
            setNetworkByProvider(source, instanceGroup, instanceGroupNetworkV4Request, subnetId);
            setupEndpointGatewayNetwork(instanceGroup.getNetwork(), stack, instanceGroup.getName(), environment);
        }
    }

    private void setNetworkByProvider(StackV4Request source, InstanceGroupV4Request instanceGroup, InstanceGroupNetworkV4Request instanceGroupNetworkV4Request,
            String subnetId) {
        switch (source.getCloudPlatform()) {
            case AWS -> setUpAws(instanceGroup, instanceGroupNetworkV4Request, subnetId);
            case AZURE -> setUpAzure(instanceGroup, instanceGroupNetworkV4Request, subnetId);
            case GCP -> setUpGcp(instanceGroup, instanceGroupNetworkV4Request, subnetId);
            case YARN -> setUpYarn(instanceGroup, instanceGroupNetworkV4Request);
            case MOCK -> setUpMock(instanceGroup, instanceGroupNetworkV4Request, subnetId);
            default -> {
            }
        }
    }

    private void setUpMock(InstanceGroupV4Request instanceGroup, InstanceGroupNetworkV4Request instanceGroupNetworkV4Request, String subnetId) {
        if (!isEmpty(subnetId) && instanceGroup.getNetwork() == null) {
            InstanceGroupMockNetworkV4Parameters mock = new InstanceGroupMockNetworkV4Parameters();
            mock.setSubnetIds(List.of(subnetId));
            instanceGroupNetworkV4Request.setMock(mock);
            instanceGroup.setNetwork(instanceGroupNetworkV4Request);
        }
    }

    private void setUpYarn(InstanceGroupV4Request instanceGroup, InstanceGroupNetworkV4Request instanceGroupNetworkV4Request) {
        if (instanceGroup.getNetwork() == null) {
            InstanceGroupYarnNetworkV4Parameters yarn = new InstanceGroupYarnNetworkV4Parameters();
            instanceGroupNetworkV4Request.setYarn(yarn);
            instanceGroup.setNetwork(instanceGroupNetworkV4Request);
        }
    }

    private void setUpGcp(InstanceGroupV4Request instanceGroup, InstanceGroupNetworkV4Request instanceGroupNetworkV4Request, String subnetId) {
        if (!isEmpty(subnetId) && instanceGroup.getNetwork() == null) {
            InstanceGroupGcpNetworkV4Parameters gcp = new InstanceGroupGcpNetworkV4Parameters();
            gcp.setSubnetIds(List.of(subnetId));
            instanceGroupNetworkV4Request.setGcp(gcp);
            instanceGroup.setNetwork(instanceGroupNetworkV4Request);
        }
    }

    private void setUpAzure(InstanceGroupV4Request instanceGroup, InstanceGroupNetworkV4Request instanceGroupNetworkV4Request, String subnetId) {
        InstanceGroupNetworkV4Request network = instanceGroup.getNetwork();
        if (StringUtils.isNotEmpty(subnetId)) {
            InstanceGroupAzureNetworkV4Parameters azure;
            if (network != null && network.getAzure() != null) {
                azure = network.getAzure();
            } else {
                azure = new InstanceGroupAzureNetworkV4Parameters();
            }
            if (CollectionUtils.isEmpty(azure.getSubnetIds())) {
                azure.setSubnetIds(List.of(subnetId));
            }
            instanceGroupNetworkV4Request.setAzure(azure);
            instanceGroup.setNetwork(instanceGroupNetworkV4Request);
        }

    }

    private void setUpAws(InstanceGroupV4Request instanceGroup, InstanceGroupNetworkV4Request instanceGroupNetworkV4Request, String subnetId) {
        if (!isEmpty(subnetId) && instanceGroup.getNetwork() == null) {
            InstanceGroupAwsNetworkV4Parameters aws = new InstanceGroupAwsNetworkV4Parameters();
            aws.setSubnetIds(List.of(subnetId));
            instanceGroupNetworkV4Request.setAws(aws);
            instanceGroup.setNetwork(instanceGroupNetworkV4Request);
        }
    }

    private String getStackSubnetIdIfExists(Stack stack) {
        return Optional.ofNullable(stack.getNetwork())
                .map(Network::getAttributes)
                .map(Json::getMap)
                .map(attr -> attr.get(SUBNET_ID))
                .map(Object::toString)
                .orElse(null);
    }

    private String getStackEndpointGatwaySubnetIdIfExists(Stack stack) {
        return Optional.ofNullable(stack.getNetwork())
                .map(Network::getAttributes)
                .map(Json::getMap)
                .map(attr -> attr.get(NetworkConstants.ENDPOINT_GATEWAY_SUBNET_ID))
                .map(Object::toString)
                .orElse(null);
    }

    private void setupEndpointGatewayNetwork(InstanceGroupNetworkV4Request instanceGroupNetworkV4Request, Stack stack, String instanceGroupName,
            DetailedEnvironmentResponse environment) {
        if (CloudPlatform.AWS.name().equals(stack.getCloudPlatform()) && instanceGroupNetworkV4Request != null &&
                instanceGroupNetworkV4Request.getAws() != null) {
            EnvironmentNetworkResponse envNetwork = environment == null ? null : environment.getNetwork();
            if (envNetwork != null) {
                if (PublicEndpointAccessGateway.ENABLED.equals(envNetwork.getPublicEndpointAccessGateway()) || isTargetingEndpointGateway(envNetwork)) {
                    LOGGER.info("Found AWS stack with endpoint gateway enabled. Selecting endpoint gateway subnet ids.");
                    List<String> subnetIds = instanceGroupNetworkV4Request.getAws().getSubnetIds();
                    LOGGER.debug("Endpoint gateway selection: Found instance group network subnet list of {} for instance group {}",
                            subnetIds, instanceGroupName);
                    List<String> allAvailabilityZones = envNetwork.getSubnetMetas().values().stream()
                            .filter(subnetMeta -> subnetIds.contains(subnetMeta.getId()))
                            .map(CloudSubnet::getAvailabilityZone)
                            .collect(Collectors.toList());
                    LOGGER.debug("Endpoint gatway selection: Instance group network has availability zones {}", allAvailabilityZones);
                    List<String> endpointGatewaySubnetIds = envNetwork.getGatewayEndpointSubnetMetas().values().stream()
                            .filter(subnetMeta -> allAvailabilityZones.contains(subnetMeta.getAvailabilityZone()))
                            .map(CloudSubnet::getId)
                            .collect(Collectors.toList());

                    if (endpointGatewaySubnetIds.isEmpty()) {
                        String endpointGatewaySubnetId = getStackEndpointGatwaySubnetIdIfExists(stack);
                        LOGGER.info("Unable to find endpoint gateway subnet metas to match AZs {}. Falling back to subnet {}.",
                                allAvailabilityZones, endpointGatewaySubnetId);
                        instanceGroupNetworkV4Request.getAws().setEndpointGatewaySubnetIds(List.of(endpointGatewaySubnetId));
                    } else {
                        LOGGER.info("Selected endpoint gateway subnets {} for instance group {}", endpointGatewaySubnetIds, instanceGroupName);
                        instanceGroupNetworkV4Request.getAws().setEndpointGatewaySubnetIds(endpointGatewaySubnetIds);
                    }
                }
            }
        }
    }

    private boolean isTargetingEndpointGateway(EnvironmentNetworkResponse network) {
        return entitlementService.isTargetingSubnetsForEndpointAccessGatewayEnabled(ThreadBasedUserCrnProvider.getAccountId()) &&
                CollectionUtils.isNotEmpty(network.getEndpointGatewaySubnetIds());
    }

    private void setNetworkIfApplicable(StackV4Request source, Stack stack, DetailedEnvironmentResponse environment) {
        if (source.getNetwork() != null) {
            source.getNetwork().setCloudPlatform(source.getCloudPlatform());
            Network network = networkV4RequestToNetworkConverter.convert(source.getNetwork());
            EnvironmentNetworkResponse envNetwork = environment == null ? null : environment.getNetwork();
            if (envNetwork != null) {
                network.setNetworkCidrs(envNetwork.getNetworkCidrs());
                network.setOutboundInternetTraffic(envNetwork.getOutboundInternetTraffic());
            }
            stack.setNetwork(network);
        } else {
            EnvironmentNetworkConverter environmentNetworkConverter = environmentNetworkConverterMap.get(source.getCloudPlatform());
            String availabilityZone = source.getPlacement() != null ? source.getPlacement().getAvailabilityZone() : null;
            if (environmentNetworkConverter != null && environment != null) {
                Network network = environmentNetworkConverter.convertToLegacyNetwork(environment.getNetwork(), availabilityZone);
                stack.setNetwork(network);
            }
        }
    }

    private void determineServiceTypeTag(Stack stack, TagsV4Request tags) {
        determineMeteringServiceFieldTag(stack, tags, ClusterTemplateApplicationTag.SERVICE_TYPE.key(), "type");
    }

    private void determineServiceFeatureTag(Stack stack, TagsV4Request tags) {
        determineMeteringServiceFieldTag(stack, tags, ClusterTemplateApplicationTag.SERVICE_FEATURE.key(), "feature");
    }

    private void determineMeteringServiceFieldTag(Stack stack, TagsV4Request tags, String tagName, String field) {
        if (tags != null && tags.getApplication() != null
                && tags.getApplication().containsKey(tagName)) {
            LOGGER.debug("The following service {} tag is provided for the cluster template: {}",
                    field, tags.getApplication().get(tagName));
        } else {
            updateMeteringServiceFieldApplicationTag(stack, tagName, field);
        }
    }

    private void updateMeteringServiceFieldApplicationTag(Stack stack, String tagName, String field) {
        try {
            if (!StackType.DATALAKE.equals(stack.getType()) && stack.getCluster() != null && stack.getCluster().getBlueprint() != null) {
                Blueprint blueprint = stack.getCluster().getBlueprint();
                String serviceField = null;
                if (ClusterTemplateApplicationTag.SERVICE_TYPE.key().equals(tagName)) {
                    serviceField = meteringServiceFieldResolver.resolveServiceType(cmTemplateProcessorFactory.get(blueprint.getBlueprintJsonText()));
                } else if (ClusterTemplateApplicationTag.SERVICE_FEATURE.key().equals(tagName)) {
                    serviceField = meteringServiceFieldResolver.resolveServiceFeature(cmTemplateProcessorFactory.get(blueprint.getBlueprintJsonText()));
                }
                if (stack.getTags() != null && serviceField != null) {
                    StackTags tags = stack.getTags().get(StackTags.class);
                    tags.getApplicationTags().put(tagName, serviceField);
                    stack.setTags(new Json(tags));
                }
            }
        } catch (IOException e) {
            throw new BadRequestException(format("Failed to convert dynamic tags for updating stack tags with service %s. Error: %s",
                    field, e.getMessage()), e);
        }
    }
}
