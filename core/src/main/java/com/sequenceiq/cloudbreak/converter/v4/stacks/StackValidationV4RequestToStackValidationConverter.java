package com.sequenceiq.cloudbreak.converter.v4.stacks;

import static com.sequenceiq.cloudbreak.converter.util.ExceptionMessageFormatterUtil.formatAccessDeniedMessage;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import javax.inject.Inject;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.HostGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackValidationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.cloudbreak.api.model.SpecialParameters;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.ClusterDefinition;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.stack.StackValidation;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.clusterdefinition.ClusterDefinitionService;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentViewService;
import com.sequenceiq.cloudbreak.service.network.NetworkService;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterCache;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@Component
public class StackValidationV4RequestToStackValidationConverter extends AbstractConversionServiceAwareConverter<StackValidationV4Request, StackValidation> {

    @Inject
    private ClusterDefinitionService clusterDefinitionService;

    @Inject
    private NetworkService networkService;

    @Inject
    private CredentialService credentialService;

    @Inject
    private EnvironmentViewService environmentViewService;

    @Inject
    private ConverterUtil converterUtil;

    @Inject
    private CloudParameterCache cloudParameterCache;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private UserService userService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    public StackValidation convert(StackValidationV4Request stackValidationRequest) {
        StackValidation stackValidation = new StackValidation();
        Set<InstanceGroup> instanceGroups = converterUtil.convertAllAsSet(stackValidationRequest.getInstanceGroups(), InstanceGroup.class);
        stackValidation.setInstanceGroups(instanceGroups);
        stackValidation.setHostGroups(convertHostGroupsFromJson(instanceGroups, stackValidationRequest.getHostGroups()));

        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        formatAccessDeniedMessage(
                () -> validateClusterDefinition(stackValidationRequest, stackValidation, workspace),
                "clusterdefinition", stackValidationRequest.getClusterDefinitionName()
        );
        formatAccessDeniedMessage(
                () -> validateEnvironment(stackValidationRequest, stackValidation, workspace),
                "environment", stackValidationRequest.getEnvironmentName()
        );
        formatAccessDeniedMessage(
                () -> validateCredential(stackValidationRequest, stackValidation, workspace),
                "credential", stackValidationRequest.getCredentialName()
        );
        formatAccessDeniedMessage(
                () -> validateNetwork(stackValidationRequest.getNetworkId(), stackValidationRequest.getNetwork(), stackValidation),
                "network",
                stackValidationRequest.getNetworkId()
        );
        return stackValidation;
    }

    private void validateNetwork(Long networkId, NetworkV4Request networkRequest, StackValidation stackValidation) {
        SpecialParameters specialParameters =
                cloudParameterCache.getPlatformParameters().get(Platform.platform(stackValidation.getCredential().cloudPlatform())).specialParameters();
        if (networkId != null) {
            Network network = networkService.get(networkId);
            stackValidation.setNetwork(network);
        } else if (networkRequest != null) {
            Network network = converterUtil.convert(networkRequest, Network.class);
            stackValidation.setNetwork(network);
        } else if (specialParameters.getSpecialParameters().get(PlatformParametersConsts.NETWORK_IS_MANDATORY)) {
            throw new BadRequestException("Network is not configured for the validation request!");
        }
    }

    private void validateEnvironment(StackValidationV4Request stackValidationRequest, StackValidation stackValidation, Workspace workspace) {
        if (!StringUtils.isEmpty(stackValidationRequest.getEnvironmentName())) {
            EnvironmentView environment = environmentViewService.getByNameForWorkspace(stackValidationRequest.getEnvironmentName(), workspace);
            stackValidation.setEnvironment(environment);
            stackValidation.setCredential(environment.getCredential());
        }
    }

    private void validateCredential(StackValidationV4Request stackValidationRequest, StackValidation stackValidation, Workspace workspace) {
        if (stackValidationRequest.getCredentialName() != null) {
            Credential credential = credentialService.getByNameForWorkspace(stackValidationRequest.getCredentialName(), workspace);
            stackValidation.setCredential(credential);
        } else if (stackValidation.getCredential() == null) {
            throw new BadRequestException("Credential is not configured for the validation request!");
        }
    }

    private void validateClusterDefinition(StackValidationV4Request stackValidationRequest, StackValidation stackValidation, Workspace workspace) {
        Set<ClusterDefinition> allAvailableInWorkspace = clusterDefinitionService.getAllAvailableInWorkspace(workspace);
        if (stackValidationRequest.getClusterDefinitionName() == null) {
            throw new BadRequestException("Cluster definition is not configured for the validation request!");
        }
        if (stackValidationRequest.getClusterDefinitionName() != null) {
            selectClusterDefinition(allAvailableInWorkspace, stackValidation, cd -> cd.getName().equals(stackValidationRequest.getClusterDefinitionName()));
        }
    }

    private Set<HostGroup> convertHostGroupsFromJson(Collection<InstanceGroup> instanceGroups, Iterable<HostGroupV4Request> hostGroupsJsons) {
        Set<HostGroup> hostGroups = new HashSet<>();
        for (HostGroupV4Request json : hostGroupsJsons) {
            HostGroup hostGroup = new HostGroup();
            hostGroup.setName(json.getName());
            String instanceGroupName = json.getInstanceGroupName();
            if (instanceGroupName != null) {
                Optional<InstanceGroup> instanceGroup =
                        instanceGroups.stream().filter(instanceGroup1 -> instanceGroup1.getGroupName().equals(instanceGroupName)).findFirst();
                if (!instanceGroup.isPresent()) {
                    throw new BadRequestException(String.format("Cannot find instance group named '%s' in instance group list", instanceGroupName));
                }
            }
            hostGroups.add(hostGroup);
        }
        return hostGroups;
    }

    private void selectClusterDefinition(Set<ClusterDefinition> clusterDefinitions, StackValidation stackValidation, Predicate<ClusterDefinition> predicate) {
        clusterDefinitions.stream()
                .filter(predicate)
                .findFirst().ifPresent(stackValidation::setClusterDefinition);
    }
}
