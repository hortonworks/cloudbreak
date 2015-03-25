package com.sequenceiq.cloudbreak.converter;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.controller.json.TemplateResponse;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.validation.GccTemplateParam;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.GccTemplate;

@Component
public class GcpTemplateToJsonConverter extends AbstractConversionServiceAwareConverter<GccTemplate, TemplateResponse> {

    @Override
    public TemplateResponse convert(GccTemplate entity) {
        TemplateResponse gccTemplateJson = new TemplateResponse();
        gccTemplateJson.setName(entity.getName());
        gccTemplateJson.setCloudPlatform(CloudPlatform.GCC);
        gccTemplateJson.setId(entity.getId());
        gccTemplateJson.setVolumeCount(entity.getVolumeCount());
        gccTemplateJson.setVolumeSize(entity.getVolumeSize());
        gccTemplateJson.setDescription(entity.getDescription());
        gccTemplateJson.setPublicInAccount(entity.isPublicInAccount());
        Map<String, Object> props = new HashMap<>();
        putProperty(props, GccTemplateParam.INSTANCETYPE.getName(), entity.getGccInstanceType());
        putProperty(props, GccTemplateParam.TYPE.getName(), entity.getGccRawDiskType());
        gccTemplateJson.setParameters(props);
        gccTemplateJson.setDescription(entity.getDescription() == null ? "" : entity.getDescription());
        return gccTemplateJson;
    }

    private void putProperty(Map<String, Object> props, String key, Object property) {
        if (property != null) {
            props.put(key, property.toString());
        }
    }
}
