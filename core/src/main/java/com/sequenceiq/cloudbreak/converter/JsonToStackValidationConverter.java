package com.sequenceiq.cloudbreak.converter;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.BYOS;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.google.common.collect.FluentIterable;
import com.sequenceiq.cloudbreak.api.model.HostGroupRequest;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupRequest;
import com.sequenceiq.cloudbreak.api.model.StackValidationRequest;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Constraint;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.StackValidation;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.service.network.NetworkService;

@Component
public class JsonToStackValidationConverter extends AbstractConversionServiceAwareConverter<StackValidationRequest, StackValidation> {

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private NetworkService networkService;

    @Inject
    private CredentialService credentialService;

    @Inject
    @Qualifier("conversionService")
    private ConversionService conversionService;

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
                    String.format("Access to blueprint '%s' is denied or blueprint doesn't exist.", stackValidationRequest.getBlueprintId()), e);
        }
        try {
            validateCredential(stackValidationRequest, stackValidation);
        } catch (AccessDeniedException e) {
            throw new AccessDeniedException(
                    String.format("Access to network '%s' is denied or network doesn't exist.", stackValidationRequest.getNetworkId()), e);
        }
        try {
            validateNetwork(stackValidationRequest, stackValidation);
        } catch (AccessDeniedException e) {
            throw new AccessDeniedException(
                    String.format("Access to network '%s' is denied or network doesn't exist.", stackValidationRequest.getNetworkId()), e);
        }
        return stackValidation;
    }

    private void validateNetwork(StackValidationRequest stackValidationRequest, StackValidation stackValidation) {
        if (stackValidationRequest.getNetworkId() != null) {
            Network network = networkService.get(stackValidationRequest.getNetworkId());
            stackValidation.setNetwork(network);
        } else if (stackValidationRequest.getNetwork() != null) {
            Network network = conversionService.convert(stackValidationRequest.getNetwork(), Network.class);
            stackValidation.setNetwork(network);
        } else if (!BYOS.equals(stackValidation.getCredential().cloudPlatform())) {
            throw new BadRequestException("Network does not configured for the validation request!");
        }
    }

    private void validateCredential(StackValidationRequest stackValidationRequest, StackValidation stackValidation) {
        if (stackValidationRequest.getCredentialId() != null) {
            Credential credential = credentialService.get(stackValidationRequest.getCredentialId());
            stackValidation.setCredential(credential);
        } else if (stackValidationRequest.getCredential() != null) {
            Credential credential = conversionService.convert(stackValidationRequest.getCredential(), Credential.class);
            stackValidation.setCredential(credential);
        } else {
            throw new BadRequestException("Credential does not configured for the validation request!");
        }
    }

    private void validateBlueprint(StackValidationRequest stackValidationRequest, StackValidation stackValidation) {
        if (stackValidationRequest.getBlueprintId() != null) {
            Blueprint blueprint = blueprintService.get(stackValidationRequest.getBlueprintId());
            stackValidation.setBlueprint(blueprint);
        } else if (stackValidationRequest.getBlueprint() != null) {
            Blueprint blueprint = conversionService.convert(stackValidationRequest.getBlueprint(), Blueprint.class);
            stackValidation.setBlueprint(blueprint);
        } else {
            throw new BadRequestException("Blueprint does not configured for the validation request!");
        }
    }

    private Set<HostGroup> convertHostGroupsFromJson(Set<InstanceGroup> instanceGroups, final Set<HostGroupRequest> hostGroupsJsons) {
        Set<HostGroup> hostGroups = new HashSet<>();
        for (final HostGroupRequest json : hostGroupsJsons) {
            HostGroup hostGroup = new HostGroup();
            hostGroup.setName(json.getName());
            Constraint constraint = getConversionService().convert(json.getConstraint(), Constraint.class);
            final String instanceGroupName = json.getConstraint().getInstanceGroupName();
            if (instanceGroupName != null) {
                InstanceGroup instanceGroup =
                        FluentIterable.from(instanceGroups).firstMatch(instanceGroup1 -> instanceGroup1.getGroupName().equals(instanceGroupName)).get();
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
