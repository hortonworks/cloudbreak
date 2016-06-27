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

import com.sequenceiq.cloudbreak.api.model.InstanceGroupJson;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.model.StackRequest;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.cloud.model.StackParamValidation;
import com.sequenceiq.cloudbreak.common.type.OrchestratorConstants;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.FailurePolicy;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.StackParameterService;

@Component
public class JsonToStackConverter extends AbstractConversionServiceAwareConverter<StackRequest, Stack> {

    @Inject
    private ConversionService conversionService;

    @Inject
    private StackParameterService stackParameterService;

    @Override
    public Stack convert(StackRequest source) {
        Stack stack = new Stack();
        stack.setName(source.getName());
        stack.setRegion(source.getRegion());
        stack.setAvailabilityZone(source.getAvailabilityZone());
        stack.setOnFailureActionAction(source.getOnFailureAction());
        stack.setStatus(Status.REQUESTED);
        stack.setInstanceGroups(convertInstanceGroups(source, stack));
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
            for (StackParamValidation stackParamValidation : stackParameterService.getStackParams(stackRequest)) {
                String paramName = stackParamValidation.getName();
                String value = userParams.get(paramName);
                if (value != null) {
                    params.put(paramName, value);
                }
            }
        }
        return params;
    }

    private Set<InstanceGroup> convertInstanceGroups(StackRequest source, Stack stack) {
        List<InstanceGroupJson> instanceGroupJsons = source.getInstanceGroups();
        Set<InstanceGroup> convertedSet = (Set<InstanceGroup>) getConversionService().convert(instanceGroupJsons, TypeDescriptor.forObject(instanceGroupJsons),
                TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(InstanceGroup.class)));
        boolean gatewaySpecified = false;
        for (InstanceGroup instanceGroup : convertedSet) {
            instanceGroup.setStack(stack);
            if (!gatewaySpecified) {
                if (InstanceGroupType.GATEWAY.equals(instanceGroup.getInstanceGroupType())) {
                    gatewaySpecified = true;
                }
            } else if (InstanceGroupType.GATEWAY.equals(instanceGroup.getInstanceGroupType())) {
                throw new BadRequestException("Only 1 Ambari server can be specified");
            }
        }
        boolean orchestratorIsMarathon = OrchestratorConstants.MARATHON.equals(source.getOrchestrator().getType());
        if (!gatewaySpecified && !orchestratorIsMarathon) {
            throw new BadRequestException("Ambari server must be specified");
        }
        return convertedSet;
    }
}
