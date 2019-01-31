package com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.InstanceGroupParameterRequest;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;

@Component
public class InstanceGroupToInstanceGroupParameterRequestConverter
        extends AbstractConversionServiceAwareConverter<InstanceGroup, InstanceGroupParameterRequest> {

    @Override
    public InstanceGroupParameterRequest convert(InstanceGroup source) {
        InstanceGroupParameterRequest request = new InstanceGroupParameterRequest();
        request.setStackName(source.getStack().getName());
        request.setGroupName(source.getGroupName());
        request.setNodeCount(source.getNodeCount());
        return request;
    }
}
