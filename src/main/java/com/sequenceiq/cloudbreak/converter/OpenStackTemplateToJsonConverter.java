package com.sequenceiq.cloudbreak.converter;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.controller.json.TemplateResponse;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.validation.OpenStackTemplateParam;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.OpenStackTemplate;

@Component
public class OpenStackTemplateToJsonConverter extends AbstractConversionServiceAwareConverter<OpenStackTemplate, TemplateResponse> {

    @Override
    public TemplateResponse convert(OpenStackTemplate source) {
        TemplateResponse json = new TemplateResponse();
        json.setName(source.getName());
        json.setCloudPlatform(CloudPlatform.OPENSTACK);
        json.setId(source.getId());
        json.setPublicInAccount(source.isPublicInAccount());
        Map<String, Object> props = new HashMap<>();
        props.put(OpenStackTemplateParam.INSTANCE_TYPE.getName(), source.getInstanceType());
        props.put(OpenStackTemplateParam.PUBLIC_NET_ID.getName(), source.getPublicNetId());
        json.setParameters(props);
        json.setDescription(source.getDescription() == null ? "" : source.getDescription());
        json.setVolumeCount(source.getVolumeCount());
        json.setVolumeSize(source.getVolumeSize());
        return json;
    }
}
