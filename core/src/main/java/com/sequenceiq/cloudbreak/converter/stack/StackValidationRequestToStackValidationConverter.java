package com.sequenceiq.cloudbreak.converter.stack;

import static com.sequenceiq.cloudbreak.converter.util.ExceptionMessageFormatterUtil.formatAccessDeniedMessage;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.api.model.NetworkRequest;
import com.sequenceiq.cloudbreak.api.model.SpecialParameters;
import com.sequenceiq.cloudbreak.api.model.stack.StackValidationRequest;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.host.HostGroupRequest;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupRequest;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Constraint;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.stack.StackValidation;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentViewService;
import com.sequenceiq.cloudbreak.service.network.NetworkService;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterCache;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@Component
public class StackValidationRequestToStackValidationConverter extends AbstractConversionServiceAwareConverter<StackValidationRequest, StackValidation> {

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private NetworkService networkService;

    @Inject
    private CredentialService credentialService;

    @Inject
    private EnvironmentViewService environmentViewService;

    @Inject
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Inject
    private CloudParameterCache cloudParameterCache;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private UserService userService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    public StackValidation convert(StackValidationRequest stackValidationRequest) {
        StackValidation stackValidation = new StackValidation();
        Set<InstanceGroup> instanceGroups = convertInstanceGroups(stackValidationRequest.getInstanceGroups());
        stackValidation.setInstanceGroups(instanceGroups);
        stackValidation.setHostGroups(convertHostGroupsFromJson(instanceGroups, stackValidationRequest.getHostGroups()));

        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        formatAccessDeniedMessage(
                () -> validateBlueprint(stackValidationRequest, stackValidation, workspace), "blueprint", stackValidationRequest.getBlueprintId()
        );
        formatAccessDeniedMessage(
                () -> {
                    validateEnvironment(stackValidationRequest, stackValidation, workspace);
                }, "environment", stackValidationRequest.getEnvironment()
        );
        formatAccessDeniedMessage(
                () -> {
                    validateCredential(stackValidationRequest, stackValidation, workspace);
                }, "credential", stackValidationRequest.getCredentialId()
        );
        formatAccessDeniedMessage(
                () -> validateNetwork(stackValidationRequest.getNetworkId(), stackValidationRequest.getNetwork(), stackValidation),
                "network",
                stackValidationRequest.getNetworkId()
        );
        return stackValidation;
    }

    private void validateNetwork(Long networkId, NetworkRequest networkRequest, StackValidation stackValidation) {
        SpecialParameters specialParameters =
                cloudParameterCache.getPlatformParameters().get(Platform.platform(stackValidation.getCredential().cloudPlatform())).specialParameters();
        if (networkId != null) {
            Network network = networkService.get(networkId);
            stackValidation.setNetwork(network);
        } else if (networkRequest != null) {
            Network network = conversionService.convert(networkRequest, Network.class);
            stackValidation.setNetwork(network);
        } else if (specialParameters.getSpecialParameters().get(PlatformParametersConsts.NETWORK_IS_MANDATORY)) {
            throw new BadRequestException("Network is not configured for the validation request!");
        }
    }

    private void validateEnvironment(StackValidationRequest stackValidationRequest, StackValidation stackValidation, Workspace workspace) {
        if (!StringUtils.isEmpty(stackValidationRequest.getEnvironment())) {
            EnvironmentView environment = environmentViewService.getByNameForWorkspace(stackValidationRequest.getEnvironment(), workspace);
            stackValidation.setEnvironment(environment);
            stackValidation.setCredential(environment.getCredential());
        }
    }

    private void validateCredential(StackValidationRequest stackValidationRequest, StackValidation stackValidation, Workspace workspace) {
        if (stackValidation.getCredential() != null) {
            return;
        } else if (stackValidationRequest.getCredentialId() != null) {
            Credential credential = credentialService.get(stackValidationRequest.getCredentialId(), workspace);
            stackValidation.setCredential(credential);
        } else if (stackValidationRequest.getCredentialName() != null) {
            Credential credential = credentialService.getByNameForWorkspace(stackValidationRequest.getCredentialName(), workspace);
            stackValidation.setCredential(credential);
        } else if (stackValidationRequest.getCredential() != null) {
            Credential credential = conversionService.convert(stackValidationRequest.getCredential(), Credential.class);
            stackValidation.setCredential(credential);
        } else if (stackValidation.getCredential() == null) {
            throw new BadRequestException("Credential is not configured for the validation request!");
        }
    }

    private void validateBlueprint(StackValidationRequest stackValidationRequest, StackValidation stackValidation, Workspace workspace) {
        Set<Blueprint> blueprintsInWorkspace = blueprintService.getAllAvailableInWorkspace(workspace);
        if (stackValidationRequest.getBlueprintId() == null
                && stackValidationRequest.getBlueprintName() == null
                && stackValidationRequest.getBlueprint() == null) {
            throw new BadRequestException("Blueprint is not configured for the validation request!");
        }
        if (stackValidationRequest.getBlueprintId() != null) {
            selectBlueprint(blueprintsInWorkspace, stackValidation, bp -> bp.getId().equals(stackValidationRequest.getBlueprintId()));
        } else if (stackValidationRequest.getBlueprintName() != null) {
            selectBlueprint(blueprintsInWorkspace, stackValidation, bp -> bp.getName().equals(stackValidationRequest.getBlueprintName()));
        } else if (stackValidationRequest.getBlueprint() != null) {
            stackValidation.setBlueprint(conversionService.convert(stackValidationRequest.getBlueprint(), Blueprint.class));
        }
        if (stackValidation.getBlueprint() == null) {
            throw new NotFoundException(String.format("Blueprint could not be validated by id: %d, name: %s or bp text",
                    stackValidationRequest.getBlueprintId(),
                    stackValidationRequest.getBlueprintName()));
        }
    }

    private Set<HostGroup> convertHostGroupsFromJson(Collection<InstanceGroup> instanceGroups, Iterable<HostGroupRequest> hostGroupsJsons) {
        Set<HostGroup> hostGroups = new HashSet<>();
        for (HostGroupRequest json : hostGroupsJsons) {
            HostGroup hostGroup = new HostGroup();
            hostGroup.setName(json.getName());
            Constraint constraint = conversionService.convert(json.getConstraint(), Constraint.class);
            String instanceGroupName = json.getConstraint().getInstanceGroupName();
            if (instanceGroupName != null) {
                Optional<InstanceGroup> instanceGroup =
                        instanceGroups.stream().filter(instanceGroup1 -> instanceGroup1.getGroupName().equals(instanceGroupName)).findFirst();
                if (!instanceGroup.isPresent()) {
                    throw new BadRequestException(String.format("Cannot find instance group named '%s' in instance group list", instanceGroupName));
                }
                constraint.setInstanceGroup(instanceGroup.get());
            }
            hostGroup.setConstraint(constraint);
            hostGroups.add(hostGroup);
        }
        return hostGroups;
    }

    private void selectBlueprint(Set<Blueprint> blueprints, StackValidation stackValidation, Predicate<Blueprint> predicate) {
        blueprints.stream()
                .filter(predicate)
                .findFirst().ifPresent(stackValidation::setBlueprint);
    }

    private Set<InstanceGroup> convertInstanceGroups(Set<InstanceGroupRequest> instanceGroupRequests) {
        return (Set<InstanceGroup>) conversionService.convert(instanceGroupRequests, TypeDescriptor.forObject(instanceGroupRequests),
                TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(InstanceGroup.class)));
    }
}
