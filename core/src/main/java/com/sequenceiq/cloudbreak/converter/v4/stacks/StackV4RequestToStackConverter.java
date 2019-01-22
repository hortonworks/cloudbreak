package com.sequenceiq.cloudbreak.converter.v4.stacks;

import static com.gs.collections.impl.utility.StringIterate.isEmpty;
import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;
import static org.apache.commons.lang3.StringUtils.isNoneEmpty;

import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.TagsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.StackInputs;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.common.service.DefaultCostTaggingService;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentViewService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

public class StackV4RequestToStackConverter extends AbstractConversionServiceAwareConverter<StackV4Request, Stack> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackV4RequestToStackConverter.class);

    @Inject
    private UserService userService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private CredentialService credentialService;

    @Inject
    private EnvironmentViewService environmentViewService;

    @Inject
    private DefaultCostTaggingService defaultCostTaggingService;

    @Inject
    private StackService stackService;

    @Value("${cb.platform.default.regions:}")
    private String defaultRegions;

    @Override
    public Stack convert(StackV4Request source) {
        validateStackAuthentication(source);

        Stack stack = new Stack();
        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        User user = userService.getOrCreate(cloudbreakUser);
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);

        String cloudPlatform = determineCloudPlatform(source, workspace);
        stack.setName(source.getName());
        stack.setDisplayName(source.getName());
        stack.setRegion(getRegion(source, cloudPlatform));
        stack.setCloudPlatform(cloudPlatform);
        stack.setTags(getTags(source, cloudPlatform));
        stack.setInputs(getInputs(source));
        stack.setDatalakeId(getSharedClusterNameOrDatalakeName(source, workspace));
        stack.setStackAuthentication(getConversionService().convert(source.getAuthentication(), StackAuthentication.class));
        stack.setAvailabilityZone(source.getEnvironment().getPlacement().getAvailabilityZone());
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.PROVISION_REQUESTED));
        stack.setCreated(Calendar.getInstance().getTimeInMillis());
        stack.setPlatformVariant(cloudPlatform);
        stack.setOrchestrator(getOrchestrator());
        updateGeneral(source, stack, workspace);
        stack.setInstanceGroups(convertInstanceGroups(source, stack));
        updateCluster(source, stack);
        if (source.getNetwork() != null) {
            stack.setNetwork(getConversionService().convert(source.getNetwork(), Network.class));
        }
        stack.setCustomDomain(source.getCustomDomain().getDomainName());
        stack.setCustomHostname(source.getCustomDomain().getHostname());
        stack.setClusterNameAsSubdomain(source.getCustomDomain().isClusterNameAsSubdomain());
        stack.setHostgroupNameAsHostname(source.getCustomDomain().isHostgroupNameAsHostname());
        stack.setGatewayPort(source.getGatewayPort());
        stack.setUuid(UUID.randomUUID().toString());
        return stack;
    }

    private Orchestrator getOrchestrator() {
        Orchestrator orchestrator = new Orchestrator();
        orchestrator.setType("SALT");
        return orchestrator;
    }

    private String getRegion(StackV4Request source, String cloudPlatform) {
        if (isEmpty(source.getEnvironment().getPlacement().getRegion())) {
            Map<Platform, Region> regions = Maps.newHashMap();
            if (isNoneEmpty(defaultRegions)) {
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
        return source.getEnvironment().getPlacement().getRegion();
    }

    public String determineCloudPlatform(StackV4Request source, Workspace workspace) {

        return StringUtils.isEmpty(source.getEnvironment().getName())
                ? credentialService.getByNameForWorkspace(source.getEnvironment().getCredentialName(), workspace).cloudPlatform()
                : environmentViewService.getByNameForWorkspace(source.getEnvironment().getName(), workspace).getCloudPlatform();
    }

    private Json getTags(StackV4Request source, String cloudPlatform) {
        try {
            TagsV4Request tags = source.getTags();
            if (tags == null) {
                return new Json(new StackTags(new HashMap<>(), new HashMap<>(), new HashMap<>()));
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

    private Json getInputs(StackV4Request source) {
        try {
            if (source.getInputs() == null) {
                return new Json(new StackInputs(new HashMap<>(), new HashMap<>(), new HashMap<>()));
            }
            return new Json(new StackInputs(source.getInputs(), new HashMap<>(), new HashMap<>()));
        } catch (Exception ignored) {
            throw new BadRequestException("Failed to convert dynamic inputs.");
        }
    }

    private Long getSharedClusterNameOrDatalakeName(StackV4Request source, Workspace workspace) {
        String name = "";
        if (!StringUtils.isEmpty(source.getDatalakeName())) {
            name = source.getDatalakeName();
        } else if (source.getCluster().getSharedService() != null) {
            name = source.getCluster().getSharedService().getSharedClusterName();
        }
        if (StringUtils.isEmpty(name)) {
            return stackService.getByNameInWorkspace(name, workspace.getId()).getId();
        }
        return null;
    }

    private void validateStackAuthentication(StackV4Request source) {
        if (source.getAuthentication() == null) {
            throw new BadRequestException("You should define authentication for stack!");
        } else if (Strings.isNullOrEmpty(source.getAuthentication().getPublicKey())
                && Strings.isNullOrEmpty(source.getAuthentication().getPublicKeyId())) {
            throw new BadRequestException("You should define the publickey or publickeyid!");
        } else if (source.getAuthentication().getLoginUserName() != null) {
            throw new BadRequestException("You can not modify the default user!");
        }
    }

    private void updateGeneral(StackV4Request source, Stack stack, Workspace workspace) {
        if (!StringUtils.isEmpty(source.getEnvironment().getCredentialName())) {
            Credential credential = credentialService.getByNameForWorkspace(source.getEnvironment().getCredentialName(), workspace);
            stack.setCredential(credential);
        }
        if (!StringUtils.isEmpty(source.getEnvironment().getName())) {
            EnvironmentView environment = environmentViewService.getByNameForWorkspace(source.getEnvironment().getName(), workspace);
            stack.setEnvironment(environment);
        }
    }

    private Set<InstanceGroup> convertInstanceGroups(StackV4Request source, Stack stack) {
        if (source.getInstanceGroups() == null) {
            return null;
        }
        Set<InstanceGroup> convertedSet = new HashSet<>();
        source.getInstanceGroups().stream()
                .map(ig -> getConversionService().convert(ig, InstanceGroup.class))
                .forEach(ig -> {
                    ig.setStack(stack);
                    convertedSet.add(ig);
                });
        return convertedSet;
    }

    private void updateCluster(StackV4Request source, Stack stack) {
        if (source.getCluster() != null) {
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
}
