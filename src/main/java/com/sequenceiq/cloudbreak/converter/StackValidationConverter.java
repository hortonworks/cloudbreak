package com.sequenceiq.cloudbreak.converter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.StackValidationRequest;
import com.sequenceiq.cloudbreak.domain.StackValidation;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.repository.BlueprintRepository;

@Component
public class StackValidationConverter {
    @Autowired
    private InstanceGroupConverter instanceGroupConverter;

    @Autowired
    private BlueprintRepository blueprintRepository;

    public StackValidation convert(StackValidationRequest stackValidationRequest) {
        StackValidation stackValidation = new StackValidation();
        stackValidation.setInstanceGroups(instanceGroupConverter.convertAllJsonToEntity(stackValidationRequest.getInstanceGroups()));
        try {
            Blueprint blueprint = blueprintRepository.findOne(stackValidationRequest.getBlueprintId());
            stackValidation.setBlueprint(blueprint);
        } catch (AccessDeniedException e) {
            throw new AccessDeniedException(
                    String.format("Access to blueprint '%s' is denied or blueprint doesn't exist.", stackValidationRequest.getBlueprintId()), e);
        }
        return stackValidation;
    }
}
