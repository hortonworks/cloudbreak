package com.sequenceiq.cloudbreak.converter;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.json.HostGroupJson;
import com.sequenceiq.cloudbreak.controller.json.StackValidationRequest;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.StackValidation;
import com.sequenceiq.cloudbreak.repository.BlueprintRepository;

@Component
public class StackValidationConverter {
    @Autowired
    private InstanceGroupConverter instanceGroupConverter;

    @Autowired
    private BlueprintRepository blueprintRepository;

    public StackValidation convert(StackValidationRequest stackValidationRequest) {
        StackValidation stackValidation = new StackValidation();
        Set<InstanceGroup> instanceGroups = instanceGroupConverter.convertAllJsonToEntity(stackValidationRequest.getInstanceGroups());
        stackValidation.setInstanceGroups(instanceGroups);
        stackValidation.setHostGroups(convertHostGroupsFromJson(instanceGroups, stackValidationRequest.getHostGroups()));
        try {
            Blueprint blueprint = blueprintRepository.findOne(stackValidationRequest.getBlueprintId());
            stackValidation.setBlueprint(blueprint);
        } catch (AccessDeniedException e) {
            throw new AccessDeniedException(
                    String.format("Access to blueprint '%s' is denied or blueprint doesn't exist.", stackValidationRequest.getBlueprintId()), e);
        }
        return stackValidation;
    }

    private Set<HostGroup> convertHostGroupsFromJson(Set<InstanceGroup> instanceGroups, final Set<HostGroupJson> hostGroupsJsons) {
        Set<HostGroup> hostGroups = new HashSet<>();
        for (final HostGroupJson json : hostGroupsJsons) {
            HostGroup hostGroup = new HostGroup();
            hostGroup.setName(json.getName());
            InstanceGroup instanceGroup = FluentIterable.from(instanceGroups).firstMatch(new Predicate<InstanceGroup>() {
                @Override
                public boolean apply(@Nullable InstanceGroup instanceGroup) {
                    return instanceGroup.getGroupName().equals(json.getInstanceGroupName());
                }
            }).get();
            if (instanceGroup == null) {
                throw new BadRequestException(String.format("Cannot find instance group named '%s' in instance group list", json.getInstanceGroupName()));
            }
            hostGroup.setInstanceGroup(instanceGroup);
            hostGroups.add(hostGroup);
        }
        return hostGroups;
    }
}
