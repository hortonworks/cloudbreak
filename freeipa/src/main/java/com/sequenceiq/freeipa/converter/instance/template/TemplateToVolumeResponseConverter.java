package com.sequenceiq.freeipa.converter.instance.template;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.VolumeResponse;
import com.sequenceiq.freeipa.entity.Template;

@Component
public class TemplateToVolumeResponseConverter implements Converter<Template, VolumeResponse> {

    @Override
    public VolumeResponse convert(Template source) {
        VolumeResponse response = new VolumeResponse();
        response.setCount(source.getVolumeCount());
        response.setSize(source.getVolumeSize());
        response.setType(source.getVolumeType());
        return response;
    }
}
