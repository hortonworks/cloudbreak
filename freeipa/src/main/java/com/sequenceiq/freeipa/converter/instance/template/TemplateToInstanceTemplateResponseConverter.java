package com.sequenceiq.freeipa.converter.instance.template;

import java.util.Set;

import javax.inject.Inject;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceTemplateResponse;
import com.sequenceiq.freeipa.entity.Template;

@Component
public class TemplateToInstanceTemplateResponseConverter implements Converter<Template, InstanceTemplateResponse> {

    @Inject
    private TemplateToVolumeResponseConverter volumeResponseConverter;

    @Override
    public InstanceTemplateResponse convert(Template source) {
        InstanceTemplateResponse response = new InstanceTemplateResponse();
        response.setAttachedVolumes(Set.of(volumeResponseConverter.convert(source)));
        response.setInstanceType(source.getInstanceType());
        return response;
    }
}
