package com.sequenceiq.cloudbreak.converter;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.TemplateJson;
import com.sequenceiq.cloudbreak.controller.validation.AwsTemplateParam;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;

@Component
public class AwsTemplateToJsonConverter extends AbstractConversionServiceAwareConverter<AwsTemplate, TemplateJson> {
    @Override public TemplateJson convert(AwsTemplate source) {
        TemplateJson templateJson = new TemplateJson();
        templateJson.setId(source.getId());
        templateJson.setName(source.getName());
        templateJson.setVolumeCount(source.getVolumeCount());
        templateJson.setVolumeSize(source.getVolumeSize());
        templateJson.setPublicInAccount(source.isPublicInAccount());
        Map<String, Object> props = new HashMap<>();
        props.put(AwsTemplateParam.INSTANCE_TYPE.getName(), source.getInstanceType().name());
        props.put(AwsTemplateParam.SSH_LOCATION.getName(), source.getSshLocation());
        props.put(AwsTemplateParam.VOLUME_TYPE.getName(), source.getVolumeType());
        props.put(AwsTemplateParam.ENCRYPTED.getName(), source.isEncrypted());
        if (source.getSpotPrice() != null) {
            props.put(AwsTemplateParam.SPOT_PRICE.getName(), source.getSpotPrice());
        }
        templateJson.setParameters(props);
        templateJson.setCloudPlatform(CloudPlatform.AWS);
        templateJson.setDescription(source.getDescription() == null ? "" : source.getDescription());
        return templateJson;
    }
}
