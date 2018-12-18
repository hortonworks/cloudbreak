package com.sequenceiq.cloudbreak.converter.v2;

import static com.gs.collections.impl.utility.StringIterate.isEmpty;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.v2.ImageSettings;
import com.sequenceiq.cloudbreak.api.model.v2.InstanceGroupV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.StackV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.Tags;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.StackInputs;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackType;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentViewService;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@Component
public class StackV2RequestToStackConverter extends AbstractConversionServiceAwareConverter<StackV2Request, Stack> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackV2RequestToStackConverter.class);

    @Inject
    private CredentialService credentialService;

    @Inject
    private EnvironmentViewService environmentViewService;

    @Inject
    private UserService userService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private CloudParameterService cloudParameterService;

    @Override
    public Stack convert(StackV2Request source) {
        Stack stack = new Stack();

        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        User user = userService.getOrCreate(cloudbreakUser);
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        stack.setWorkspace(workspace);

        Map<String, String> parameters = new HashMap<>();
        source.getParameters().forEach((key, value) -> parameters.put(key, value.toString()));
        stack.setParameters(parameters);

        updateGeneral(source, stack, workspace);

        if (source.getPlacement() != null) {
            stack.setAvailabilityZone(source.getPlacement().getAvailabilityZone());
            stack.setRegion(source.getPlacement().getRegion());
        }
        stack.setPlatformVariant(source.getPlatformVariant());
        updatePlatform(stack);
        stack.setTags(getTags(source.getTags()));
        stack.setInputs(getInputs(source.getInputs()));
        if (source.getStackAuthentication() != null) {
            StackAuthentication stackAuthentication = getConversionService().convert(source.getStackAuthentication(), StackAuthentication.class);
            stack.setStackAuthentication(stackAuthentication);
        }
        stack.setInstanceGroups(convertInstanceGroups(source, stack));
        if (source.getNetwork() != null) {
            stack.setNetwork(getConversionService().convert(source.getNetwork(), Network.class));
        }
        updateCustomDomain(source, stack);

        updateCluster(source, stack);

        if (source.getImageSettings() != null) {
            stack.getComponents().add(getImageComponent(source, stack));
        }

        stack.setType(StackType.TEMPLATE);
        return stack;
    }

    private void updateGeneral(StackV2Request source, Stack stack, Workspace workspace) {
        if (source.getGeneral() != null) {
            stack.setName(source.getGeneral().getName());
            stack.setDisplayName(source.getGeneral().getName());
            if (source.getGeneral().getCredentialName() != null) {
                Credential credential = credentialService.getByNameForWorkspace(source.getGeneral().getCredentialName(), workspace);
                stack.setCredential(credential);
                stack.setCloudPlatform(credential.cloudPlatform());
            }
            if (source.getGeneral().getEnvironmentName() != null) {
                EnvironmentView environment = environmentViewService.getByNameForWorkspace(source.getGeneral().getEnvironmentName(), workspace);
                stack.setEnvironment(environment);
                stack.setCloudPlatform(environment.getCloudPlatform());
            }
        }
    }

    private void updateCluster(StackV2Request source, Stack stack) {
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

    private void updateCustomDomain(StackV2Request source, Stack stack) {
        if (source.getCustomDomain() != null) {
            stack.setCustomDomain(source.getCustomDomain().getCustomDomain());
            stack.setCustomHostname(source.getCustomDomain().getCustomHostname());
            stack.setClusterNameAsSubdomain(source.getCustomDomain().isClusterNameAsSubdomain());
            stack.setHostgroupNameAsHostname(source.getCustomDomain().isHostgroupNameAsHostname());
        }
    }

    private Json getTags(Tags tags) {
        if (tags == null) {
            return null;
        }
        return Json.silent(new StackTags(tags.getUserDefinedTags(), tags.getApplicationTags(), tags.getDefaultTags()));
    }

    private Json getInputs(Map<String, Object> inputs) {
        return Json.silent(new StackInputs(inputs, new HashMap<>(), new HashMap<>()));
    }

    private void updatePlatform(Stack stack) {
        if (isEmpty(stack.cloudPlatform()) && !isEmpty(stack.getPlatformVariant())) {
            String cloudPlatform = cloudParameterService.getPlatformByVariant(stack.getPlatformVariant());
            stack.setCloudPlatform(cloudPlatform);
        }
    }

    private Set<InstanceGroup> convertInstanceGroups(StackV2Request source, Stack stack) {
        if (source.getInstanceGroups() == null) {
            return null;
        }
        List<InstanceGroupV2Request> instanceGroupRequests = source.getInstanceGroups();
        Set<InstanceGroup> convertedSet = new HashSet<>();
        instanceGroupRequests.stream()
                .map(ig -> getConversionService().convert(ig, InstanceGroup.class))
                .forEach(ig -> {
                    ig.setStack(stack);
                    convertedSet.add(ig);
                });
        return convertedSet;
    }

    private com.sequenceiq.cloudbreak.domain.stack.Component getImageComponent(StackV2Request source, Stack stack) {
        ImageSettings imageSettings = source.getImageSettings();
        Image image = new Image(null, null, imageSettings.getOs(), null, null, imageSettings.getImageCatalog(),
                imageSettings.getImageId(), null);
        return new com.sequenceiq.cloudbreak.domain.stack.Component(ComponentType.IMAGE, ComponentType.IMAGE.name(), Json.silent(image), stack);
    }
}

