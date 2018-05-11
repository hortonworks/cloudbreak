package com.sequenceiq.cloudbreak.converter.stack;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.stack.cluster.host.HostGroupRequest;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupRequest;
import com.sequenceiq.cloudbreak.api.model.NetworkRequest;
import com.sequenceiq.cloudbreak.api.model.SpecialParameters;
import com.sequenceiq.cloudbreak.api.model.stack.StackValidationRequest;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Constraint;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.stack.StackValidation;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.service.network.NetworkService;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterCache;

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

    @Override
    public StackValidation convert(StackValidationRequest stackValidationRequest) {
        StackValidation stackValidation = new StackValidation();
        Set<InstanceGroup> instanceGroups = convertInstanceGroups(stackValidationRequest.getInstanceGroups());
        stackValidation.setInstanceGroups(instanceGroups);
        stackValidation.setHostGroups(convertHostGroupsFromJson(instanceGroups, stackValidationRequest.getHostGroups()));
        try {
            validateBlueprint(stackValidationRequest, stackValidation);
        } catch (AccessDeniedException e) {
            throw new AccessDeniedException(
                    String.format("Access to validation '%s' is denied or validation doesn't exist.", stackValidationRequest.getBlueprintId()), e);
        }
        try {
            validateCredential(stackValidationRequest, stackValidation);
        } catch (AccessDeniedException e) {
            throw new AccessDeniedException(
                    String.format("Access to credential '%s' is denied or credential doesn't exist.", stackValidationRequest.getCredentialId()), e);
        }
        try {
            validateNetwork(stackValidationRequest.getNetworkId(), stackValidationRequest.getNetwork(), stackValidation);
        } catch (AccessDeniedException e) {
            throw new AccessDeniedException(
                    String.format("Access to network '%s' is denied or network doesn't exist.", stackValidationRequest.getNetworkId()), e);
        }
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

    private void validateCredential(StackValidationRequest stackValidationRequest, StackValidation stackValidation) {
        if (stackValidationRequest.getCredentialId() != null) {
            Credential credential = credentialService.get(stackValidationRequest.getCredentialId());
            stackValidation.setCredential(credential);
        } else if (stackValidationRequest.getCredentialName() != null) {
            Credential credential = credentialService.get(stackValidationRequest.getCredentialName(), stackValidationRequest.getAccount());
            stackValidation.setCredential(credential);
        } else if (stackValidationRequest.getCredential() != null) {
            Credential credential = conversionService.convert(stackValidationRequest.getCredential(), Credential.class);
            stackValidation.setCredential(credential);
        } else {
            throw new BadRequestException("Credential is not configured for the validation request!");
        }
    }

    private void validateBlueprint(StackValidationRequest stackValidationRequest, StackValidation stackValidation) {
        if (stackValidationRequest.getBlueprintId() != null) {
            Blueprint blueprint = blueprintService.get(stackValidationRequest.getBlueprintId());
            stackValidation.setBlueprint(blueprint);
        } else if (stackValidationRequest.getBlueprintName() != null) {
            Blueprint blueprint = blueprintService.get(stackValidationRequest.getBlueprintName(), stackValidationRequest.getAccount());
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
                InstanceGroup instanceGroup =
                        instanceGroups.stream().filter(instanceGroup1 -> instanceGroup1.getGroupName().equals(instanceGroupName)).findFirst().get();
                if (instanceGroup == null) {
                    throw new BadRequestException(String.format("Cannot find instance group named '%s' in instance group list", instanceGroupName));
                }
                constraint.setInstanceGroup(instanceGroup);
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
