package com.sequenceiq.cloudbreak.converter;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.CredentialJson;
import com.sequenceiq.cloudbreak.controller.validation.AWSCredentialParam;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;

@Component
public class AwsCredentialConverter extends AbstractConverter<CredentialJson, AwsCredential> {

    @Override
    public CredentialJson convert(AwsCredential entity) {
        CredentialJson credentialJson = new CredentialJson();
        credentialJson.setId(entity.getId());
        credentialJson.setCloudPlatform(CloudPlatform.AWS);
        credentialJson.setName(entity.getName());
        Map<String, Object> params = new HashMap<>();
        params.put(AWSCredentialParam.ROLE_ARN.getName(), entity.getRoleArn());
        params.put(AWSCredentialParam.INSTANCE_PROFILE_ROLE_ARN.getName(), entity.getInstanceProfileRoleArn());
        params.put(AWSCredentialParam.NOTIFICATION_ARN.getName(), entity.getNotificationArn());
        credentialJson.setParameters(params);
        credentialJson.setDescription(entity.getDescription() == null ? "" : entity.getDescription());
        return credentialJson;
    }

    @Override
    public AwsCredential convert(CredentialJson json) {
        AwsCredential awsCredential = new AwsCredential();
        awsCredential.setName(json.getName());
        awsCredential.setRoleArn(String.valueOf(json.getParameters().get(AWSCredentialParam.ROLE_ARN.getName())));
        awsCredential.setInstanceProfileRoleArn(String.valueOf(json.getParameters().get(AWSCredentialParam.INSTANCE_PROFILE_ROLE_ARN.getName())));
        awsCredential.setCloudPlatform(CloudPlatform.AWS);
        awsCredential.setDescription(json.getDescription());
        return awsCredential;
    }
}
