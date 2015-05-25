package com.sequenceiq.cloudbreak.converter;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.TemplateResponse;
import com.sequenceiq.cloudbreak.controller.validation.GcpTemplateParam;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.GcpTemplate;

@Component
public class GcpTemplateToJsonConverter extends AbstractConversionServiceAwareConverter<GcpTemplate, TemplateResponse> {

    @Override
    public TemplateResponse convert(GcpTemplate entity) {
        TemplateResponse gcpTemplateJson = new TemplateResponse();
        gcpTemplateJson.setName(entity.getName());
        gcpTemplateJson.setCloudPlatform(CloudPlatform.GCP);
        gcpTemplateJson.setId(entity.getId());
        gcpTemplateJson.setVolumeCount(entity.getVolumeCount());
        gcpTemplateJson.setVolumeSize(entity.getVolumeSize());
        gcpTemplateJson.setDescription(entity.getDescription());
        gcpTemplateJson.setPublicInAccount(entity.isPublicInAccount());
        Map<String, Object> props = new HashMap<>();
        putProperty(props, GcpTemplateParam.INSTANCETYPE.getName(), entity.getGcpInstanceType());
        putProperty(props, GcpTemplateParam.TYPE.getName(), entity.getGcpRawDiskType());
        gcpTemplateJson.setParameters(props);
        gcpTemplateJson.setDescription(entity.getDescription() == null ? "" : entity.getDescription());
        return gcpTemplateJson;
    }

    private void putProperty(Map<String, Object> props, String key, Object property) {
        if (property != null) {
            props.put(key, property.toString());
        }
    }
}
