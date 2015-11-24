package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.type.ResourceStatus;
import com.sequenceiq.cloudbreak.controller.json.TemplateRequest;
import com.sequenceiq.cloudbreak.controller.validation.GcpTemplateParam;
import com.sequenceiq.cloudbreak.domain.GcpTemplate;

@Component
public class JsonToGcpTemplateConverter extends AbstractConversionServiceAwareConverter<TemplateRequest, GcpTemplate> {

    @Override
    public GcpTemplate convert(TemplateRequest source) {
        GcpTemplate gcpTemplate = new GcpTemplate();
        gcpTemplate.setName(source.getName());
        gcpTemplate.setDescription(source.getDescription());
        gcpTemplate.setStatus(ResourceStatus.USER_MANAGED);
        gcpTemplate.setVolumeCount((source.getVolumeCount() == null) ? 0 : source.getVolumeCount());
        gcpTemplate.setVolumeSize((source.getVolumeSize() == null) ? 0 : source.getVolumeSize());
        gcpTemplate.setInstanceType(source.getParameters().get(GcpTemplateParam.INSTANCETYPE.getName()).toString());
        Object type = source.getParameters().get(GcpTemplateParam.TYPE.getName());
        if (type != null) {
            gcpTemplate.setGcpRawDiskType(type.toString());
        }
        gcpTemplate.setDescription(source.getDescription());
        return gcpTemplate;
    }
}
