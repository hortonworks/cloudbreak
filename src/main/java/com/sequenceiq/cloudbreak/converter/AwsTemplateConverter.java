package com.sequenceiq.cloudbreak.converter;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.VolumeType;
import com.sequenceiq.cloudbreak.controller.json.TemplateJson;
import com.sequenceiq.cloudbreak.controller.validation.AwsTemplateParam;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;

@Component
public class AwsTemplateConverter extends AbstractConverter<TemplateJson, AwsTemplate> {

    private static final String DEFAULT_SSH_LOCATION = "0.0.0.0/0";

    @Override
    public TemplateJson convert(AwsTemplate entity) {
        TemplateJson templateJson = new TemplateJson();
        templateJson.setId(entity.getId());
        templateJson.setName(entity.getName());
        templateJson.setVolumeCount(entity.getVolumeCount());
        templateJson.setVolumeSize(entity.getVolumeSize());
        templateJson.setPublicInAccount(entity.isPublicInAccount());
        Map<String, Object> props = new HashMap<>();
        props.put(AwsTemplateParam.INSTANCE_TYPE.getName(), entity.getInstanceType().name());
        props.put(AwsTemplateParam.SSH_LOCATION.getName(), entity.getSshLocation());
        props.put(AwsTemplateParam.VOLUME_TYPE.getName(), entity.getVolumeType());
        if (entity.getSpotPrice() != null) {
            props.put(AwsTemplateParam.SPOT_PRICE.getName(), entity.getSpotPrice());
        }
        templateJson.setParameters(props);
        templateJson.setCloudPlatform(CloudPlatform.AWS);
        templateJson.setPublicInAccount(entity.isPublicInAccount());
        templateJson.setDescription(entity.getDescription() == null ? "" : entity.getDescription());
        return templateJson;
    }

    @Override
    public AwsTemplate convert(TemplateJson json) {
        AwsTemplate awsTemplate = new AwsTemplate();
        awsTemplate.setName(json.getName());
        awsTemplate.setInstanceType(InstanceType.valueOf(String.valueOf(json.getParameters().get(AwsTemplateParam.INSTANCE_TYPE.getName()))));
        String sshLocation = json.getParameters().containsKey(AwsTemplateParam.SSH_LOCATION.getName())
                ? String.valueOf(json.getParameters().get(AwsTemplateParam.SSH_LOCATION.getName())) : DEFAULT_SSH_LOCATION;
        awsTemplate.setSshLocation(sshLocation);
        awsTemplate.setDescription(json.getDescription());
        awsTemplate.setVolumeCount((json.getVolumeCount() == null) ? 0 : json.getVolumeCount());
        awsTemplate.setVolumeSize((json.getVolumeSize() == null) ? 0 : json.getVolumeSize());
        awsTemplate.setVolumeType(VolumeType.valueOf(String.valueOf(json.getParameters().get(AwsTemplateParam.VOLUME_TYPE.getName()))));
        Double spotPrice = json.getParameters().containsKey(AwsTemplateParam.SPOT_PRICE.getName())
                && json.getParameters().get(AwsTemplateParam.SPOT_PRICE.getName()) != null
                ? Double.valueOf(json.getParameters().get(AwsTemplateParam.SPOT_PRICE.getName()).toString()) : null;
        awsTemplate.setSpotPrice(spotPrice);
        return awsTemplate;
    }

    public AwsTemplate convert(TemplateJson json, boolean publicInAccount) {
        AwsTemplate awsTemplate = convert(json);
        awsTemplate.setPublicInAccount(publicInAccount);
        return awsTemplate;
    }
}
