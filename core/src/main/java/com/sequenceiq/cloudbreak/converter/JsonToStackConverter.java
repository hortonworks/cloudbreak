package com.sequenceiq.cloudbreak.converter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.InstanceGroupJson;
import com.sequenceiq.cloudbreak.controller.json.StackRequest;
import com.sequenceiq.cloudbreak.controller.validation.StackParam;
import com.sequenceiq.cloudbreak.domain.FailurePolicy;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;

@Component
public class JsonToStackConverter extends AbstractConversionServiceAwareConverter<StackRequest, Stack> {

    @Override
    public Stack convert(StackRequest source) {
        Stack stack = new Stack();
        stack.setName(source.getName());
        stack.setRegion(source.getRegion());
        stack.setOnFailureActionAction(source.getOnFailureAction());
        stack.setStatus(Status.REQUESTED);
        stack.setInstanceGroups(convertInstanceGroups(source.getInstanceGroups(), stack));
        if (source.getImage() != null) {
            stack.setImage(source.getImage());
        }
        stack.setFailurePolicy(getConversionService().convert(source.getFailurePolicy(), FailurePolicy.class));
        stack.setParameters(getValidParameters(source));
        return stack;
    }

    private Map<String, String> getValidParameters(StackRequest stackRequest) {
        Map<String, String> params = new HashMap<>();
        Map<String, String> userParams = stackRequest.getParameters();
        if (userParams != null) {
            for (StackParam stackParam : StackParam.values()) {
                String paramName = stackParam.getName();
                String value = userParams.get(paramName);
                if (value != null) {
                    params.put(paramName, value);
                }
            }
        }
        return params;
    }

    private Set<InstanceGroup> convertInstanceGroups(List<InstanceGroupJson> instanceGroupJsons, Stack stack) {
        Set<InstanceGroup> convertedSet = (Set<InstanceGroup>) getConversionService().convert(instanceGroupJsons, TypeDescriptor.forObject(instanceGroupJsons),
                TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(InstanceGroup.class)));
        for (InstanceGroup instanceGroup : convertedSet) {
            instanceGroup.setStack(stack);
        }
        return convertedSet;
    }
}
