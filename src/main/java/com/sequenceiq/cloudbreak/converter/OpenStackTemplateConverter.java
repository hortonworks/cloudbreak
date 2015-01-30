package com.sequenceiq.cloudbreak.converter;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.TemplateJson;
import com.sequenceiq.cloudbreak.controller.validation.OpenStackTemplateParam;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.OpenStackTemplate;

@Component
public class OpenStackTemplateConverter extends AbstractConverter<TemplateJson, OpenStackTemplate> {

    @Override
    public TemplateJson convert(OpenStackTemplate entity) {
        TemplateJson json = new TemplateJson();
        json.setName(entity.getName());
        json.setCloudPlatform(CloudPlatform.OPENSTACK);
        json.setId(entity.getId());
        json.setPublicInAccount(entity.isPublicInAccount());
        Map<String, Object> props = new HashMap<>();
        props.put(OpenStackTemplateParam.INSTANCE_TYPE.getName(), entity.getInstanceType());
        props.put(OpenStackTemplateParam.PUBLIC_NET_ID.getName(), entity.getPublicNetId());
        json.setParameters(props);
        json.setDescription(entity.getDescription() == null ? "" : entity.getDescription());
        json.setVolumeCount(entity.getVolumeCount());
        json.setVolumeSize(entity.getVolumeSize());
        return json;
    }

    @Override
    public OpenStackTemplate convert(TemplateJson json) {
        OpenStackTemplate template = new OpenStackTemplate();
        template.setName(json.getName());
        template.setInstanceType(String.valueOf(json.getParameters().get(OpenStackTemplateParam.INSTANCE_TYPE.getName())));
        template.setPublicNetId(String.valueOf(json.getParameters().get(OpenStackTemplateParam.PUBLIC_NET_ID.getName())));
        template.setDescription(json.getDescription());
        template.setVolumeCount((json.getVolumeCount() == null) ? 0 : json.getVolumeCount());
        template.setVolumeSize((json.getVolumeSize() == null) ? 0 : json.getVolumeSize());
        return template;
    }

    public OpenStackTemplate convert(TemplateJson json, boolean publicInAccount) {
        OpenStackTemplate openStackTemplate = convert(json);
        openStackTemplate.setPublicInAccount(publicInAccount);
        return openStackTemplate;
    }

}
