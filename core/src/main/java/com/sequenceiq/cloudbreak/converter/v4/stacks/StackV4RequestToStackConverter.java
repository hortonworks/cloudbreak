package com.sequenceiq.cloudbreak.converter.v4.stacks;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.environment.placement.PlacementSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.TagsV4Request;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.StackInputs;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.DefaultCostTaggingService;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.converter.v4.environment.network.EnvironmentNetworkConverter;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.SecurityRule;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.datalake.DatalakeResourcesService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.gs.collections.impl.utility.StringIterate.isEmpty;
import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;
import static com.sequenceiq.cloudbreak.util.Benchmark.measure;
import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@Component
public class StackV4RequestToStackConverter extends AbstractConversionServiceAwareConverter<StackV4Request, Stack> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackV4RequestToStackConverter.class);

    private static final String TCP_PROTOCOL = "tcp";

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private EnvironmentClientService environmentClientService;

    @Inject
    private DefaultCostTaggingService defaultCostTaggingService;

    @Inject
    private ProviderParameterCalculator providerParameterCalculator;

    @Inject
    private DatalakeResourcesService datalakeResourcesService;

    @Inject
    private Clock clock;

    @Inject
    private Map<CloudPlatform, EnvironmentNetworkConverter> environmentNetworkConverterMap;

    @Inject
    private TelemetryConverter telemetryConverter;

    @Value("${cb.platform.default.regions:}")
    private String defaultRegions;

    @Value("#{'${cb.default.gateway.cidr}'.split(',')}")
    private Set<String> defaultGatewayCidr;

    @Value("${cb.nginx.port}")
    private Integer nginxPort;

    @Value("${cb.https.port}")
    private Integer httpsPort;

    @Override
    public Stack convert(StackV4Request source) {
        Workspace workspace = workspaceService.getForCurrentUser();

        Stack stack = new Stack();
        stack.setEnvironmentCrn(source.getEnvironmentCrn());
        DetailedEnvironmentResponse environment = null;
        if (isTemplate(source)) {
            if (source.getEnvironmentCrn() != null) {
                environment = measure(() -> environmentClientService.getByCrn(source.getEnvironmentCrn()),
                        LOGGER, "Environment responded in {} ms for stack {}", source.getName());
                updateCustomDomainOrKerberos(source, stack);
                updateCloudPlatformAndRelatedFields(source, stack, environment);
            }
            convertAsStackTemplate(source, stack, environment);
            setNetworkAsTemplate(source, stack);
        } else {
            environment = measure(() -> environmentClientService.getByCrn(source.getEnvironmentCrn()),
                    LOGGER, "Environment responded in {} ms for stack {}", source.getName());
            convertAsStack(source, stack);
            updateCloudPlatformAndRelatedFields(source, stack, environment);
            setNetworkIfApplicable(source, stack, environment);
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
        stack.setDatalakeResourceId(getDatalakeResourceId(source, workspace));
        stack.setStackAuthentication(getConversionService().convert(source.getAuthentication(), StackAuthentication.class));
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.PROVISION_REQUESTED));
        stack.setCreated(clock.getCurrentTimeMillis());
        stack.setInstanceGroups(convertInstanceGroups(source, stack));
        measure(() -> updateCluster(source, stack, workspace),
                LOGGER, "Converted cluster and updated the stack in {} ms for stack {}", source.getName());
        stack.getComponents().add(getTelemetryComponent(stack, source));

        stack.setGatewayPort(source.getGatewayPort());
        stack.setUuid(UUID.randomUUID().toString());
        stack.setType(source.getType());
        stack.setInputs(Json.silent(new StackInputs(source.getInputs(), new HashMap<>(), new HashMap<>())));
        if (source.getImage() != null) {
            stack.getComponents().add(getImageComponent(source, stack));
        }
        extendGatewaySecurityGroupWithDefaultGatewayCidrs(stack);
        return stack;
    }

    private void extendGatewaySecurityGroupWithDefaultGatewayCidrs(Stack stack) {
        Set<InstanceGroup> gateways =
                stack.getInstanceGroups().stream().filter(ig -> InstanceGroupType.isGateway(ig.getInstanceGroupType())).collect(Collectors.toSet());
        Set<String> defaultGatewayCidrs = defaultGatewayCidr.stream().filter(StringUtils::isNotBlank).collect(Collectors.toSet());
        if (!defaultGatewayCidrs.isEmpty()) {
            for (InstanceGroup gateway : gateways) {
                SecurityGroup securityGroup = Optional.ofNullable(gateway.getSecurityGroup()).orElse(new SecurityGroup());
                gateway.setSecurityGroup(securityGroup);
                if (CollectionUtils.isEmpty(securityGroup.getSecurityGroupIds())) {
                    Set<SecurityRule> rules = securityGroup.getSecurityRules();
                    String ports = (stack.getGatewayPort() == null ? nginxPort.toString() : stack.getGatewayPort().toString()) + ',' + httpsPort;
                    defaultGatewayCidrs.forEach(cloudbreakCidr -> rules.add(createSecurityRule(securityGroup, cloudbreakCidr, ports)));
                    LOGGER.info("The control plane cidrs {} are added to the {} gateway group for the {} port.", defaultGatewayCidrs, gateway.getGroupName(),
                            stack.getGatewayPort());
                }
            }
        }
    }

    private SecurityRule createSecurityRule(SecurityGroup securityGroup, String cidr, String ports) {
        SecurityRule securityRule = new SecurityRule();
        securityRule.setPorts(ports);
        securityRule.setProtocol(TCP_PROTOCOL);
        securityRule.setCidr(cidr);
        securityRule.setSecurityGroup(securityGroup);
        return securityRule;
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
        if (StringUtils.isEmpty(stack.getCustomHostname())) {
            stack.setCustomHostname(stack.getName());
        }
    }

    private com.sequenceiq.cloudbreak.domain.stack.Component getTelemetryComponent(Stack stack, StackV4Request source) {
        Telemetry telemetry = telemetryConverter.convert(source.getTelemetry(), source.getType());
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
        stack.setTags(getTags(source, cloudPlatform));
        stack.setPlatformVariant(cloudPlatform);
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
                    throw new BadRequestException(String.format("No default region specified for: %s. Region cannot be empty.", cloudPlatform));
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
        return environmentResponse.getCloudPlatform();
    }

    private Json getTags(StackV4Request source, String cloudPlatform) {
        try {
            TagsV4Request tags = source.getTags();
            if (tags == null) {
                return new Json(new StackTags(new HashMap<>(), new HashMap<>(), getDefaultTags(cloudPlatform)));
            }
            return new Json(new StackTags(tags.getUserDefined(), tags.getApplication(), getDefaultTags(cloudPlatform)));
        } catch (Exception ignored) {
            throw new BadRequestException("Failed to convert dynamic tags.");
        }
    }

    private Map<String, String> getDefaultTags(String cloudPlatform) {
        Map<String, String> result = new HashMap<>();
        try {
            result.putAll(defaultCostTaggingService.prepareDefaultTags(restRequestThreadLocalService.getCloudbreakUser().getUsername(), result, cloudPlatform));
        } catch (Exception e) {
            LOGGER.debug("Exception during reading default tags.", e);
        }
        return result;
    }

    private Long getDatalakeResourceId(StackV4Request source, Workspace workspace) {
        try {
            if (source.getSharedService() != null && isNotBlank(source.getSharedService().getDatalakeName())) {
                return datalakeResourcesService.getByNameForWorkspace(source.getSharedService().getDatalakeName(), workspace).getId();
            }
        } catch (NotFoundException nfe) {
            LOGGER.debug("No datalake resource found for data lake: {}", source.getSharedService().getDatalakeName());
        }
        return null;
    }

    private void validateStackAuthentication(StackV4Request source) {
        if (Strings.isNullOrEmpty(source.getAuthentication().getPublicKey())
                && Strings.isNullOrEmpty(source.getAuthentication().getPublicKeyId())) {
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
                    return getConversionService().convert(ig, InstanceGroup.class);
                })
                .forEach(ig -> {
                    ig.setStack(stack);
                    convertedSet.add(ig);
                });
        return convertedSet;
    }

    private void updateCluster(StackV4Request source, Stack stack, Workspace workspace) {
        if (source.getCluster() != null) {
            source.getCluster().setName(stack.getName());
            Cluster cluster = getConversionService().convert(source.getCluster(), Cluster.class);
            Set<HostGroup> hostGroups = source.getInstanceGroups().stream()
                    .map(ig -> {
                        HostGroup hostGroup = getConversionService().convert(ig, HostGroup.class);
                        hostGroup.setCluster(cluster);
                        return hostGroup;
                    })
                    .collect(Collectors.toSet());
            cluster.setHostGroups(hostGroups);
            stack.setCluster(cluster);
        }
    }

    private com.sequenceiq.cloudbreak.domain.stack.Component getImageComponent(StackV4Request source, Stack stack) {
        ImageSettingsV4Request imageSettings = source.getImage();
        Image image = new Image(null, null, imageSettings.getOs(), null, null, imageSettings.getCatalog(), imageSettings.getId(), null);
        return new com.sequenceiq.cloudbreak.domain.stack.Component(ComponentType.IMAGE, ComponentType.IMAGE.name(), Json.silent(image), stack);
    }

    private void setNetworkAsTemplate(StackV4Request source, Stack stack) {
        if (source.getNetwork() != null) {
            source.getNetwork().setCloudPlatform(source.getCloudPlatform());
            stack.setNetwork(getConversionService().convert(source.getNetwork(), Network.class));
        }
    }

    private void setNetworkIfApplicable(StackV4Request source, Stack stack, DetailedEnvironmentResponse environment) {
        if (source.getNetwork() != null) {
            source.getNetwork().setCloudPlatform(source.getCloudPlatform());
            stack.setNetwork(getConversionService().convert(source.getNetwork(), Network.class));
        } else {
            EnvironmentNetworkConverter environmentNetworkConverter = environmentNetworkConverterMap.get(source.getCloudPlatform());
            String availabilityZone = source.getPlacement() != null ? source.getPlacement().getAvailabilityZone() : null;
            if (environmentNetworkConverter != null) {
                Network network = environmentNetworkConverter.convertToLegacyNetwork(environment.getNetwork(), availabilityZone);
                stack.setNetwork(network);
            }
        }
    }
}
