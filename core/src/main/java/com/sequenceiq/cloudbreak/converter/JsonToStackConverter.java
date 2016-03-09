package com.sequenceiq.cloudbreak.converter;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.controller.validation.StackParam;
import com.sequenceiq.cloudbreak.domain.FailurePolicy;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupJson;
import com.sequenceiq.cloudbreak.api.model.StackRequest;

@Component
public class JsonToStackConverter extends AbstractConversionServiceAwareConverter<StackRequest, Stack> {

    @Inject
    private ConversionService conversionService;

    @Override
    public Stack convert(StackRequest source) {
        Stack stack = new Stack();
        stack.setName(source.getName());
        stack.setRegion(source.getRegion());
        stack.setAvailabilityZone(source.getAvailabilityZone());
        stack.setOnFailureActionAction(source.getOnFailureAction());
        stack.setStatus(Status.REQUESTED);
        stack.setInstanceGroups(convertInstanceGroups(source.getInstanceGroups(), stack));
        stack.setFailurePolicy(getConversionService().convert(source.getFailurePolicy(), FailurePolicy.class));
        stack.setParameters(getValidParameters(source));
        stack.setCreated(Calendar.getInstance().getTimeInMillis());
        stack.setPlatformVariant(source.getPlatformVariant());
        stack.setOrchestrator(conversionService.convert(source.getOrchestrator(), Orchestrator.class));
        stack.setRelocateDocker(source.getRelocateDocker() == null ? true : source.getRelocateDocker());
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
