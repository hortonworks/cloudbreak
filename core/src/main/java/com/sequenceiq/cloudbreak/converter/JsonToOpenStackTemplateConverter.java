package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.cloudbreak.controller.json.TemplateRequest;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.validation.OpenStackTemplateParam;
import com.sequenceiq.cloudbreak.domain.OpenStackTemplate;

@Component
public class JsonToOpenStackTemplateConverter extends AbstractConversionServiceAwareConverter<TemplateRequest, OpenStackTemplate> {

    @Override
    public OpenStackTemplate convert(TemplateRequest source) {
        OpenStackTemplate template = new OpenStackTemplate();
        template.setName(source.getName());
        template.setInstanceType(String.valueOf(source.getParameters().get(OpenStackTemplateParam.INSTANCE_TYPE.getName())));
        template.setPublicNetId(String.valueOf(source.getParameters().get(OpenStackTemplateParam.PUBLIC_NET_ID.getName())));
        template.setDescription(source.getDescription());
        template.setVolumeCount((source.getVolumeCount() == null) ? 0 : source.getVolumeCount());
        template.setVolumeSize((source.getVolumeSize() == null) ? 0 : source.getVolumeSize());
        return template;
    }
}
