package com.sequenceiq.cloudbreak.converter;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.sequenceiq.cloudbreak.api.model.HostGroupJson;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupJson;
import com.sequenceiq.cloudbreak.api.model.StackValidationRequest;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Constraint;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.StackValidation;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.network.NetworkService;

@Component
public class JsonToStackValidationConverter extends AbstractConversionServiceAwareConverter<StackValidationRequest, StackValidation> {

    @Inject
    private BlueprintService blueprintService;
    @Inject
    private NetworkService networkService;
    @Inject
    private ConversionService conversionService;

    @Override
    public StackValidation convert(StackValidationRequest stackValidationRequest) {
        StackValidation stackValidation = new StackValidation();
        Set<InstanceGroup> instanceGroups = convertInstanceGroups(stackValidationRequest.getInstanceGroups());
        stackValidation.setInstanceGroups(instanceGroups);
        stackValidation.setHostGroups(convertHostGroupsFromJson(instanceGroups, stackValidationRequest.getHostGroups()));
        try {
            Blueprint blueprint = blueprintService.get(stackValidationRequest.getBlueprintId());
            stackValidation.setBlueprint(blueprint);
        } catch (AccessDeniedException e) {
            throw new AccessDeniedException(
                    String.format("Access to blueprint '%s' is denied or blueprint doesn't exist.", stackValidationRequest.getBlueprintId()), e);
        }
        try {
            Network network = networkService.get(stackValidationRequest.getNetworkId());
            stackValidation.setNetwork(network);
        } catch (AccessDeniedException e) {
            throw new AccessDeniedException(
                    String.format("Access to network '%s' is denied or network doesn't exist.", stackValidationRequest.getNetworkId()), e);
        }
        return stackValidation;
    }

    private Set<HostGroup> convertHostGroupsFromJson(Set<InstanceGroup> instanceGroups, final Set<HostGroupJson> hostGroupsJsons) {
        Set<HostGroup> hostGroups = new HashSet<>();
        for (final HostGroupJson json : hostGroupsJsons) {
            HostGroup hostGroup = new HostGroup();
            hostGroup.setName(json.getName());
            Constraint constraint = conversionService.convert(json.getConstraint(), Constraint.class);
            final String instanceGroupName = json.getConstraint().getInstanceGroupName();
            if (instanceGroupName != null) {
                InstanceGroup instanceGroup = FluentIterable.from(instanceGroups).firstMatch(new Predicate<InstanceGroup>() {
                    @Override
                    public boolean apply(@Nullable InstanceGroup instanceGroup) {
                        return instanceGroup.getGroupName().equals(instanceGroupName);
                    }
                }).get();
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

    private Set<InstanceGroup> convertInstanceGroups(Set<InstanceGroupJson> instanceGroupJsons) {
        return (Set<InstanceGroup>) getConversionService().convert(instanceGroupJsons, TypeDescriptor.forObject(instanceGroupJsons),
                TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(InstanceGroup.class)));
    }
}
