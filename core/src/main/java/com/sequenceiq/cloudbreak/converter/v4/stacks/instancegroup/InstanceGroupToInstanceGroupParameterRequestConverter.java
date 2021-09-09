package com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup;

import java.util.HashMap;

import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.model.InstanceGroupParameterRequest;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;

@Component
public class InstanceGroupToInstanceGroupParameterRequestConverter {

    public InstanceGroupParameterRequest convert(InstanceGroup source) {
        InstanceGroupParameterRequest instanceGroupParameterRequest = new InstanceGroupParameterRequest();
        instanceGroupParameterRequest.setGroupName(source.getGroupName());
        if (source.getAttributes() != null && !Strings.isNullOrEmpty(source.getAttributes().getValue())) {
            instanceGroupParameterRequest.setParameters(source.getAttributes().getMap());
        } else {
            instanceGroupParameterRequest.setParameters(new HashMap<>());
        }
        instanceGroupParameterRequest.setNodeCount(source.getNodeCount());
        return instanceGroupParameterRequest;
    }
}
