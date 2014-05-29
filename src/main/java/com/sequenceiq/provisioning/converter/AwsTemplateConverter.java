package com.sequenceiq.provisioning.converter;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.model.InstanceType;
import com.sequenceiq.provisioning.controller.json.TemplateJson;
import com.sequenceiq.provisioning.controller.validation.AwsTemplateParam;
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
        props.put(AwsTemplateParam.KEY_NAME.getName(), entity.getKeyName());
        props.put(AwsTemplateParam.REGION.getName(), entity.getRegion().toString());
        props.put(AwsTemplateParam.AMI_ID.getName(), entity.getAmiId());
        props.put(AwsTemplateParam.INSTANCE_TYPE.getName(), entity.getInstanceType().toString());
        props.put(AwsTemplateParam.SSH_LOCATION.getName(), entity.getSshLocation());
        templateJson.setParameters(props);
        templateJson.setCloudPlatform(CloudPlatform.AWS);
        return templateJson;
    }

    @Override
    public AwsTemplate convert(TemplateJson json) {
        AwsTemplate awsTemplate = new AwsTemplate();
        awsTemplate.setName(json.getClusterName());
        awsTemplate.setRegion(Regions.valueOf(json.getParameters().get(AwsTemplateParam.REGION.getName())));
        awsTemplate.setKeyName(json.getParameters().get(AwsTemplateParam.KEY_NAME.getName()));
        awsTemplate.setAmiId(json.getParameters().get(AwsTemplateParam.AMI_ID.getName()));
        awsTemplate.setInstanceType(InstanceType.valueOf(json.getParameters().get(AwsTemplateParam.INSTANCE_TYPE.getName())));
        String sshLocation = json.getParameters().containsKey(AwsTemplateParam.SSH_LOCATION.getName())
                ? json.getParameters().get(AwsTemplateParam.SSH_LOCATION.getName()) : DEFAULT_SSH_LOCATION;
        awsTemplate.setSshLocation(sshLocation);
        return awsTemplate;
    }
}
