package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.TemplateJson;
import com.sequenceiq.cloudbreak.controller.validation.GccTemplateParam;
import com.sequenceiq.cloudbreak.domain.GccTemplate;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.domain.GccInstanceType;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.domain.GccRawDiskType;

@Component
public class JsonToGcpTemplateConverter extends AbstractConversionServiceAwareConverter<TemplateJson, GccTemplate> {

    @Override
    public GccTemplate convert(TemplateJson source) {
        GccTemplate gccTemplate = new GccTemplate();
        gccTemplate.setName(source.getName());
        gccTemplate.setDescription(source.getDescription());
        gccTemplate.setVolumeCount((source.getVolumeCount() == null) ? 0 : source.getVolumeCount());
        gccTemplate.setVolumeSize((source.getVolumeSize() == null) ? 0 : source.getVolumeSize());
        gccTemplate.setGccInstanceType(GccInstanceType.valueOf(source.getParameters().get(GccTemplateParam.INSTANCETYPE.getName()).toString()));
        Object type = source.getParameters().get(GccTemplateParam.TYPE.getName());
        if (type != null) {
            gccTemplate.setGccRawDiskType(GccRawDiskType.valueOf(type.toString()));
        }
        gccTemplate.setDescription(source.getDescription());
        return gccTemplate;
    }
}
