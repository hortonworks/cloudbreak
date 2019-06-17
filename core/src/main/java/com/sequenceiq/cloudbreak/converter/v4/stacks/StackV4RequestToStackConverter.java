package com.sequenceiq.cloudbreak.converter.v4.stacks;

import static com.gs.collections.impl.utility.StringIterate.isEmpty;
import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.environment.placement.PlacementSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.TagsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.telemetry.TelemetryV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.telemetry.logging.LoggingV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.telemetry.workload.WorkloadAnalyticsV4Request;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.Logging;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.StackInputs;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.cloud.model.Telemetry;
import com.sequenceiq.cloudbreak.cloud.model.WorkloadAnalytics;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.DefaultCostTaggingService;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.converter.v4.environment.network.EnvironmentNetworkConverter;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.datalake.DatalakeResourcesService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.type.KerberosType;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Component
public class StackV4RequestToStackConverter extends AbstractConversionServiceAwareConverter<StackV4Request, Stack> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackV4RequestToStackConverter.class);

    @Inject
    private UserService userService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private CredentialClientService credentialClientService;

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
    private KerberosConfigService kerberosConfigService;

    @Value("${cb.platform.default.regions:}")
    private String defaultRegions;

    @Override
    public Stack convert(StackV4Request source) {
        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        User user = userService.getOrCreate(cloudbreakUser);
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);

        Stack stack = new Stack();
        DetailedEnvironmentResponse environment = environmentClientService.getByCrn(source.getEnvironmentCrn());

        if (isTemplate(source)) {
            convertAsStackTemplate(source, stack, workspace, environment);
        } else {
            convertAsStack(source, stack, workspace, environment);
        }
        updateCloudPlatformAndRelatedFields(source, stack, workspace, environment);
        Map<String, Object> asMap = providerParameterCalculator.get(source).asMap();
        if (asMap != null) {
            Map<String, String> parameter = new HashMap<>();
            asMap.forEach((key, value) -> parameter.put(key, value.toString()));
            stack.setParameters(parameter);
        }
        setTimeToLive(source, stack);
        stack.setEnvironmentCrn(source.getEnvironmentCrn());
        stack.setWorkspace(workspace);
        stack.setDisplayName(source.getName());
        stack.setDatalakeResourceId(getDatalakeResourceId(source, workspace));
        stack.setStackAuthentication(getConversionService().convert(source.getAuthentication(), StackAuthentication.class));
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.PROVISION_REQUESTED));
        stack.setCreated(clock.getCurrentTimeMillis());
        stack.setInstanceGroups(convertInstanceGroups(source, stack));
        updateCluster(source, stack, workspace);
        stack.getComponents().add(getTelemetryComponent(source, stack));
        setNetworkIfApplicable(source, stack, environment);
        if (source.getCustomDomain() != null) {
            stack.setCustomDomain(source.getCustomDomain().getDomainName());
            stack.setCustomHostname(source.getCustomDomain().getHostname());
            stack.setClusterNameAsSubdomain(source.getCustomDomain().isClusterNameAsSubdomain());
            stack.setHostgroupNameAsHostname(source.getCustomDomain().isHostgroupNameAsHostname());
        } else {
            Optional<KerberosConfig> kerberosConfig = kerberosConfigService.get(stack.getEnvironmentCrn());
            kerberosConfig.ifPresent(kb -> {
                if (kb.getType() == KerberosType.ACTIVE_DIRECTORY) {
                    if (isEmpty(kb.getRealm())) {
                        throw new BadRequestException("Realm cannot be null in case of ACTIVE_DIRECTORY");
                    }
                    stack.setCustomDomain(kb.getRealm().toLowerCase());
                }
            });
        }
        stack.setGatewayPort(source.getGatewayPort());
        stack.setUuid(UUID.randomUUID().toString());
        stack.setType(source.getType());
        stack.setInputs(Json.silent(new StackInputs(source.getInputs(), new HashMap<>(), new HashMap<>())));
        if (source.getImage() != null) {
            stack.getComponents().add(getImageComponent(source, stack));
        }
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

    private void convertAsStack(StackV4Request source, Stack stack, Workspace workspace, DetailedEnvironmentResponse environment) {
        validateStackAuthentication(source);
        stack.setName(source.getName());
        stack.setAvailabilityZone(getAvailabilityZone(Optional.ofNullable(source.getPlacement())));
        stack.setOrchestrator(getOrchestrator());
    }

    private com.sequenceiq.cloudbreak.domain.stack.Component getTelemetryComponent(StackV4Request source, Stack stack) {
        try {
            TelemetryV4Request telemetryRequest = source.getTelemetry();
            Logging logging = null;
            WorkloadAnalytics workloadAnalytics = null;
            if (telemetryRequest != null) {
                if (telemetryRequest.getLogging() != null) {
                    LoggingV4Request loggingRequest = telemetryRequest.getLogging();
                    logging = new Logging(loggingRequest.isEnabled(), loggingRequest.getOutput(), loggingRequest.getAttributes());
                }
                if (telemetryRequest.getWorkloadAnalytics() != null) {
                    WorkloadAnalyticsV4Request waRequest = telemetryRequest.getWorkloadAnalytics();
                    workloadAnalytics = new WorkloadAnalytics(waRequest.isEnabled(), waRequest.getDatabusEndpoint(),
                            waRequest.getAccessKey(), waRequest.getPrivateKey(), waRequest.getAttributes());
                }
            }
            Telemetry telemetry = new Telemetry(logging, workloadAnalytics);
            return new com.sequenceiq.cloudbreak.domain.stack.Component(ComponentType.TELEMETRY, ComponentType.TELEMETRY.name(), Json.silent(telemetry), stack);
        } catch (Exception e) {
            LOGGER.debug("Exception during reading telemetry settings.", e);
            throw new BadRequestException("Failed to convert dynamic telemetry settingss.");
        }
    }

    private void updateCloudPlatformAndRelatedFields(StackV4Request source, Stack stack, Workspace workspace, DetailedEnvironmentResponse environment) {
        String cloudPlatform = determineCloudPlatform(source, workspace, environment);
        source.setCloudPlatform(CloudPlatform.valueOf(cloudPlatform));
        stack.setRegion(getRegion(source, cloudPlatform));
        stack.setCloudPlatform(cloudPlatform);
        stack.setTags(getTags(source, cloudPlatform));
        stack.setPlatformVariant(cloudPlatform);
    }

    private void convertAsStackTemplate(StackV4Request source, Stack stack, Workspace workspace, DetailedEnvironmentResponse environment) {
        if (source.getEnvironmentCrn() != null) {
            updateCloudPlatformAndRelatedFields(source, stack, workspace, environment);
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
        if (source.getPlacement() == null) {
            return null;
        }
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

    private String determineCloudPlatform(StackV4Request source, Workspace workspace, DetailedEnvironmentResponse environmentResponse) {
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
            result.putAll(defaultCostTaggingService.prepareDefaultTags(restRequestThreadLocalService.getCloudbreakUser(), result, cloudPlatform));
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

    private void setNetworkIfApplicable(StackV4Request source, Stack stack, DetailedEnvironmentResponse environment) {
        if (source.getNetwork() != null) {
            source.getNetwork().setCloudPlatform(source.getCloudPlatform());
            stack.setNetwork(getConversionService().convert(source.getNetwork(), Network.class));
        } else {
            if (environment != null && environment.getNetwork() != null) {
                EnvironmentNetworkConverter environmentNetworkConverter = environmentNetworkConverterMap.get(source.getCloudPlatform());
                if (environmentNetworkConverter != null) {
                    Network network = environmentNetworkConverter.convertToLegacyNetwork(environment.getNetwork());
                    stack.setNetwork(network);
                }
            }
        }
    }
}
