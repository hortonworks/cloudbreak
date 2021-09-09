package com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.template;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.template.volume.VolumeV4Response;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;

@Component
public class TemplateToVolumeV4ResponseConverter {

    public VolumeV4Response convert(VolumeTemplate source) {
        VolumeV4Response response = new VolumeV4Response();
        response.setCount(source.getVolumeCount());
        response.setSize(source.getVolumeSize());
        response.setType(source.getVolumeType());
        return response;
    }
}
