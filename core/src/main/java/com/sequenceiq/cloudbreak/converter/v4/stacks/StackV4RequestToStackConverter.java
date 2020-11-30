package com.sequenceiq.cloudbreak.converter.v4.stacks;

import static com.gs.collections.impl.utility.StringIterate.isEmpty;
import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;
import static com.sequenceiq.cloudbreak.util.Benchmark.measure;
import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.environment.placement.PlacementSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.TagsV4Request;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.StackInputs;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.cmtemplate.metering.MeteringServiceFieldResolver;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.converter.v4.environment.network.EnvironmentNetworkConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.service.datalake.DatalakeResourcesService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.stack.GatewaySecurityGroupDecorator;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.tag.ClusterTemplateApplicationTag;
import com.sequenceiq.cloudbreak.tag.CostTagging;
import com.sequenceiq.cloudbreak.tag.request.CDPTagMergeRequest;
import com.sequenceiq.cloudbreak.util.PasswordUtil;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

@Component
public class StackV4RequestToStackConverter extends AbstractConversionServiceAwareConverter<StackV4Request, Stack> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackV4RequestToStackConverter.class);

    @Value("${cb.ambari.username:cloudbreak}")
    private String ambariUserName;

    @Value("${cb.cm.mgmt.username:cmmgmt}")
    private String cmMgmtUsername;

    @Value("${cb.cm.monitoring.username:cmmonitoring}")
    private String cmMonitoringUser;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private EnvironmentClientService environmentClientService;

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

    @Override
    public Stack convert(StackV4Request source) {
        Workspace workspace = workspaceService.getForCurrentUser();

        Stack stack = new Stack();
        stack.setEnvironmentCrn(source.getEnvironmentCrn());
        DetailedEnvironmentResponse environment = null;
        if (!Strings.isNullOrEmpty(source.getEnvironmentCrn())) {
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
        stack.setDatalakeResourceId(getDatalakeResourceId(source, workspace));
        stack.setStackAuthentication(getConversionService().convert(source.getAuthentication(), StackAuthentication.class));
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.PROVISION_REQUESTED));
        stack.setCreated(clock.getCurrentTimeMillis());
        stack.setInstanceGroups(convertInstanceGroups(source, stack));
        measure(() -> updateCluster(source, stack, workspace),
                LOGGER, "Converted cluster and updated the stack in {} ms for stack {}", source.getName());
        stack.setGatewayPort(source.getGatewayPort());
        stack.setUuid(UUID.randomUUID().toString());
        stack.setType(source.getType());
        stack.setInputs(Json.silent(new StackInputs(source.getInputs(), new HashMap<>(), new HashMap<>())));
        if (source.getImage() != null) {
            stack.getComponents().add(getImageComponent(source, stack));
        }
        if (!isTemplate(source)) {
            gatewaySecurityGroupDecorator.extendGatewaySecurityGroupWithDefaultGatewayCidrs(stack, environment.getTunnel());
        }
        stack.setExternalDatabaseCreationType(getIfNotNull(source.getExternalDatabase(), DatabaseRequest::getAvailabilityType));
        determineServiceTypeTag(stack, source.getTags());
        determineServiceFeatureTag(stack, source.getTags());
        return stack;
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
        stack.setTags(getTags(source, environment));
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
        return environmentResponse.getCloudPlatform();
    }

    private Json getTags(StackV4Request source, DetailedEnvironmentResponse environment) {
        try {
            TagsV4Request tags = source.getTags();
            if (tags == null) {
                Map<String, String> userDefined = environment == null ? new HashMap<>() : environment.getTags().getUserDefined();
                return new Json(new StackTags(userDefined, new HashMap<>(), new HashMap<>()));
            }

            Map<String, String> userDefined = new HashMap<>();
            if (environment.getTags() != null && environment.getTags().getUserDefined() != null && !environment.getTags().getUserDefined().isEmpty()) {
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
            fillCredentialValues(source, cluster);
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

    private void fillCredentialValues(StackV4Request source, Cluster cluster) {
        if (source.getType() != StackType.TEMPLATE) {
            cluster.setUserName(source.getCluster().getUserName());
            cluster.setPassword(source.getCluster().getPassword());
            cluster.setCloudbreakUser(ambariUserName);
            cluster.setCloudbreakPassword(PasswordUtil.generatePassword());
            cluster.setCloudbreakClusterManagerMonitoringUser(cmMonitoringUser);
            cluster.setCloudbreakClusterManagerMonitoringPassword(PasswordUtil.generatePassword());
            cluster.setDpUser(cmMgmtUsername);
            cluster.setDpPassword(PasswordUtil.generatePassword());
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
                null,
                imageSettings.getCatalog(),
                imageSettings.getId(),
                null);
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
            Network network = getConversionService().convert(source.getNetwork(), Network.class);
            EnvironmentNetworkResponse envNetwork = environment.getNetwork();
            if (envNetwork != null) {
                network.setNetworkCidrs(envNetwork.getNetworkCidrs());
                network.setOutboundInternetTraffic(envNetwork.getOutboundInternetTraffic());
            }
            stack.setNetwork(network);
        } else {
            EnvironmentNetworkConverter environmentNetworkConverter = environmentNetworkConverterMap.get(source.getCloudPlatform());
            String availabilityZone = source.getPlacement() != null ? source.getPlacement().getAvailabilityZone() : null;
            if (environmentNetworkConverter != null) {
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
                    serviceField = meteringServiceFieldResolver.resolveServiceType(cmTemplateProcessorFactory.get(blueprint.getBlueprintText()));
                } else if (ClusterTemplateApplicationTag.SERVICE_FEATURE.key().equals(tagName)) {
                    serviceField = meteringServiceFieldResolver.resolveServiceFeature(cmTemplateProcessorFactory.get(blueprint.getBlueprintText()));
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
