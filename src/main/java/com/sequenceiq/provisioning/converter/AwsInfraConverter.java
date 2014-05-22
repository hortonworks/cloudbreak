package com.sequenceiq.provisioning.converter;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.amazonaws.services.ec2.model.InstanceType;
import com.sequenceiq.provisioning.controller.json.InfraJson;
import com.sequenceiq.provisioning.controller.validation.OptionalAwsInfraParam;
import com.sequenceiq.provisioning.controller.validation.RequiredAwsInfraParam;
import com.sequenceiq.provisioning.domain.AwsInfra;
import com.sequenceiq.provisioning.domain.CloudPlatform;

@Component
public class AwsInfraConverter extends AbstractConverter<InfraJson, AwsInfra> {

    private static final String DEFAULT_SSH_LOCATION = "0.0.0.0/0";

    @Override
    public InfraJson convert(AwsInfra entity) {
        InfraJson infraJson = new InfraJson();
        infraJson.setId(entity.getId());
        infraJson.setClusterName(entity.getName());
        Map<String, String> props = new HashMap<>();
        props.put(RequiredAwsInfraParam.KEY_NAME.getName(), entity.getKeyName());
        props.put(RequiredAwsInfraParam.REGION.getName(), entity.getRegion());
        props.put(RequiredAwsInfraParam.AMI_ID.getName(), entity.getAmiId());
        props.put(RequiredAwsInfraParam.INSTANCE_TYPE.getName(), entity.getInstanceType().toString());
        props.put(OptionalAwsInfraParam.SSH_LOCATION.getName(), entity.getSshLocation());
        infraJson.setParameters(props);
        infraJson.setCloudPlatform(CloudPlatform.AWS);
        return infraJson;
    }

    @Override
    public AwsInfra convert(InfraJson json) {
        AwsInfra awsInfra = new AwsInfra();
        awsInfra.setName(json.getClusterName());
        awsInfra.setRegion(json.getParameters().get(RequiredAwsInfraParam.REGION.getName()));
        awsInfra.setKeyName(json.getParameters().get(RequiredAwsInfraParam.KEY_NAME.getName()));
        awsInfra.setAmiId(json.getParameters().get(RequiredAwsInfraParam.AMI_ID.getName()));
        awsInfra.setInstanceType(InstanceType.fromValue(json.getParameters().get(RequiredAwsInfraParam.INSTANCE_TYPE.getName())));
        String sshLocation = json.getParameters().containsKey(OptionalAwsInfraParam.SSH_LOCATION.getName())
                ? json.getParameters().get(OptionalAwsInfraParam.SSH_LOCATION.getName()) : DEFAULT_SSH_LOCATION;
        awsInfra.setSshLocation(sshLocation);
        return awsInfra;
    }
}
