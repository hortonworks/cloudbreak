package com.sequenceiq.provisioning.converter;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.amazonaws.services.ec2.model.InstanceType;
import com.sequenceiq.provisioning.controller.json.TemplateJson;
import com.sequenceiq.provisioning.controller.validation.RequiredAwsTemplateParam;
import com.sequenceiq.provisioning.domain.AwsTemplate;
import com.sequenceiq.provisioning.domain.CloudPlatform;

@Component
public class AwsTemplateConverter extends AbstractConverter<TemplateJson, AwsTemplate> {

    private static final String DEFAULT_SSH_LOCATION = "0.0.0.0/0";

    @Override
    public TemplateJson convert(AwsTemplate entity) {
        TemplateJson templateJson = new TemplateJson();
        templateJson.setId(entity.getId());
        templateJson.setClusterName(entity.getName());
        Map<String, String> props = new HashMap<>();
        props.put(RequiredAwsTemplateParam.KEY_NAME.getName(), entity.getKeyName());
        props.put(RequiredAwsTemplateParam.REGION.getName(), entity.getRegion());
        props.put(RequiredAwsTemplateParam.AMI_ID.getName(), entity.getAmiId());
        props.put(RequiredAwsTemplateParam.INSTANCE_TYPE.getName(), entity.getInstanceType().toString());
        props.put(RequiredAwsTemplateParam.SSH_LOCATION.getName(), entity.getSshLocation());
        templateJson.setParameters(props);
        templateJson.setCloudPlatform(CloudPlatform.AWS);
        return templateJson;
    }

    @Override
    public AwsTemplate convert(TemplateJson json) {
        AwsTemplate awsTemplate = new AwsTemplate();
        awsTemplate.setName(json.getClusterName());
        awsTemplate.setRegion(json.getParameters().get(RequiredAwsTemplateParam.REGION.getName()));
        awsTemplate.setKeyName(json.getParameters().get(RequiredAwsTemplateParam.KEY_NAME.getName()));
        awsTemplate.setAmiId(json.getParameters().get(RequiredAwsTemplateParam.AMI_ID.getName()));
        awsTemplate.setInstanceType(InstanceType.valueOf(json.getParameters().get(RequiredAwsTemplateParam.INSTANCE_TYPE.getName())));
        String sshLocation = json.getParameters().containsKey(RequiredAwsTemplateParam.SSH_LOCATION.getName())
                ? json.getParameters().get(RequiredAwsTemplateParam.SSH_LOCATION.getName()) : DEFAULT_SSH_LOCATION;
        awsTemplate.setSshLocation(sshLocation);
        return awsTemplate;
    }
}
