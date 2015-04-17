package com.sequenceiq.cloudbreak.converter;

import java.util.List;
import java.util.Set;

import com.sequenceiq.cloudbreak.controller.json.StackRequest;
import com.sequenceiq.cloudbreak.controller.json.SubnetJson;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.InstanceGroupJson;
import com.sequenceiq.cloudbreak.domain.FailurePolicy;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.domain.Subnet;

@Component
public class JsonToStackConverter extends AbstractConversionServiceAwareConverter<StackRequest, Stack> {

    @Override
    public Stack convert(StackRequest source) {
        Stack stack = new Stack();
        stack.setName(source.getName());
        stack.setUserName(source.getUserName());
        stack.setPassword(source.getPassword());
        stack.setRegion(source.getRegion());
        stack.setOnFailureActionAction(source.getOnFailureAction());
        if (source.getAllowedSubnets() != null) {
            stack.setAllowedSubnets(convertSubnets(source.getAllowedSubnets(), stack));
        }
        stack.setStatus(Status.REQUESTED);
        stack.setInstanceGroups(convertInstanceGroups(source.getInstanceGroups(), stack));
        if (source.getImage() != null) {
            stack.setImage(source.getImage());
        }
        stack.setFailurePolicy(getConversionService().convert(source.getFailurePolicy(), FailurePolicy.class));
        stack.setParameters(source.getParameters());
        return stack;
    }

    private Set<Subnet> convertSubnets(List<SubnetJson> source, Stack stack) {
        Set<Subnet> convertedSet = (Set<Subnet>) getConversionService().convert(source,
                TypeDescriptor.forObject(source),
                TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(Subnet.class)));
        for (Subnet subNet : convertedSet) {
            subNet.setStack(stack);
        }
        return convertedSet;
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
