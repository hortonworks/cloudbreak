package com.sequenceiq.cloudbreak.converter.v2;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.InstanceGroupRequest;
import com.sequenceiq.cloudbreak.api.model.SecurityGroupRequest;
import com.sequenceiq.cloudbreak.api.model.TemplateRequest;
import com.sequenceiq.cloudbreak.api.model.v2.InstanceGroupV2Request;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class InstanceGroupV2RequestToInstanceGroupRequestConverter
        extends AbstractConversionServiceAwareConverter<InstanceGroupV2Request, InstanceGroupRequest> {

    @Override
    public InstanceGroupRequest convert(InstanceGroupV2Request instanceGroupV2Request) {
        InstanceGroupRequest instanceGroupRequest = new InstanceGroupRequest();
        instanceGroupRequest.setTemplate(getConversionService().convert(instanceGroupV2Request.getTemplate(), TemplateRequest.class));
        instanceGroupRequest.setSecurityGroup(getConversionService().convert(instanceGroupV2Request.getSecurityGroup(), SecurityGroupRequest.class));
        instanceGroupRequest.setGroup(instanceGroupV2Request.getGroup());
        instanceGroupRequest.setNodeCount(instanceGroupV2Request.getNodeCount());
        instanceGroupRequest.setParameters(instanceGroupV2Request.getParameters());
        instanceGroupRequest.setType(instanceGroupV2Request.getType());
        return instanceGroupRequest;
    }
}
