package com.sequenceiq.cloudbreak.converter;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.model.InstanceType;
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
        Map<String, Object> props = new HashMap<>();
        props.put(AwsTemplateParam.REGION.getName(), entity.getRegion().toString());
        props.put(AwsTemplateParam.AMI_ID.getName(), entity.getAmiId());
        props.put(AwsTemplateParam.INSTANCE_TYPE.getName(), entity.getInstanceType().toString());
        props.put(AwsTemplateParam.SSH_LOCATION.getName(), entity.getSshLocation());
        templateJson.setParameters(props);
        templateJson.setCloudPlatform(CloudPlatform.AWS);
        templateJson.setDescription(entity.getDescription() == null ? "" : entity.getDescription());
        return templateJson;
    }

    @Override
    public AwsTemplate convert(TemplateJson json) {
        AwsTemplate awsTemplate = new AwsTemplate();
        awsTemplate.setName(json.getName());
        awsTemplate.setRegion(Regions.valueOf(String.valueOf(json.getParameters().get(AwsTemplateParam.REGION.getName()))));
        awsTemplate.setAmiId(String.valueOf(json.getParameters().get(AwsTemplateParam.AMI_ID.getName())));
        awsTemplate.setInstanceType(InstanceType.valueOf(String.valueOf(json.getParameters().get(AwsTemplateParam.INSTANCE_TYPE.getName()))));
        String sshLocation = json.getParameters().containsKey(AwsTemplateParam.SSH_LOCATION.getName())
                ? String.valueOf(json.getParameters().get(AwsTemplateParam.SSH_LOCATION.getName())) : DEFAULT_SSH_LOCATION;
        awsTemplate.setSshLocation(sshLocation);
        awsTemplate.setDescription(json.getDescription());
        return awsTemplate;
    }
}
