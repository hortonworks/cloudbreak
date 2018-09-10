package com.sequenceiq.cloudbreak.converter.stack;

import static com.sequenceiq.cloudbreak.converter.util.ExceptionMessageFormatterUtil.formatAccessDeniedMessage;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.NetworkRequest;
import com.sequenceiq.cloudbreak.api.model.SpecialParameters;
import com.sequenceiq.cloudbreak.api.model.stack.StackValidationRequest;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.host.HostGroupRequest;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupRequest;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Constraint;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.stack.StackValidation;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.service.network.NetworkService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterCache;
import com.sequenceiq.cloudbreak.service.user.UserService;

@Component
public class StackValidationRequestToStackValidationConverter extends AbstractConversionServiceAwareConverter<StackValidationRequest, StackValidation> {

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private NetworkService networkService;

    @Inject
    private CredentialService credentialService;

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
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    public StackValidation convert(StackValidationRequest stackValidationRequest) {
        StackValidation stackValidation = new StackValidation();
        Set<InstanceGroup> instanceGroups = convertInstanceGroups(stackValidationRequest.getInstanceGroups());
        stackValidation.setInstanceGroups(instanceGroups);
        stackValidation.setHostGroups(convertHostGroupsFromJson(instanceGroups, stackValidationRequest.getHostGroups()));

        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);

        formatAccessDeniedMessage(
                () -> validateBlueprint(stackValidationRequest, stackValidation, workspace), "blueprint", stackValidationRequest.getBlueprintId()
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

    private void validateCredential(StackValidationRequest stackValidationRequest, StackValidation stackValidation, Workspace workspace) {
        if (stackValidationRequest.getCredentialId() != null) {
            Credential credential = credentialService.get(stackValidationRequest.getCredentialId(), workspace);
            stackValidation.setCredential(credential);
        } else if (stackValidationRequest.getCredentialName() != null) {
            Credential credential = credentialService.getByNameForWorkspace(stackValidationRequest.getCredentialName(), workspace);
            stackValidation.setCredential(credential);
        } else if (stackValidationRequest.getCredential() != null) {
            Credential credential = conversionService.convert(stackValidationRequest.getCredential(), Credential.class);
            stackValidation.setCredential(credential);
        } else {
            throw new BadRequestException("Credential is not configured for the validation request!");
        }
    }

    private void validateBlueprint(StackValidationRequest stackValidationRequest, StackValidation stackValidation, Workspace workspace) {
        if (stackValidationRequest.getBlueprintId() != null) {
            Blueprint blueprint = blueprintService.get(stackValidationRequest.getBlueprintId());
            stackValidation.setBlueprint(blueprint);
        } else if (stackValidationRequest.getBlueprintName() != null) {
            Blueprint blueprint = blueprintService.getByNameForWorkspace(stackValidationRequest.getBlueprintName(), workspace);
            stackValidation.setBlueprint(blueprint);
        } else if (stackValidationRequest.getBlueprint() != null) {
            Blueprint blueprint = conversionService.convert(stackValidationRequest.getBlueprint(), Blueprint.class);
            stackValidation.setBlueprint(blueprint);
        } else {
            throw new BadRequestException("Blueprint is not configured for the validation request!");
        }
    }

    private Set<HostGroup> convertHostGroupsFromJson(Collection<InstanceGroup> instanceGroups, Iterable<HostGroupRequest> hostGroupsJsons) {
        Set<HostGroup> hostGroups = new HashSet<>();
        for (HostGroupRequest json : hostGroupsJsons) {
            HostGroup hostGroup = new HostGroup();
            hostGroup.setName(json.getName());
            Constraint constraint = getConversionService().convert(json.getConstraint(), Constraint.class);
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

    private Set<InstanceGroup> convertInstanceGroups(Set<InstanceGroupRequest> instanceGroupRequests) {
        return (Set<InstanceGroup>) getConversionService().convert(instanceGroupRequests, TypeDescriptor.forObject(instanceGroupRequests),
                TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(InstanceGroup.class)));
    }
}
