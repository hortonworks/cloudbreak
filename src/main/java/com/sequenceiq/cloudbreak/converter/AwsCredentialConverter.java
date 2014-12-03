package com.sequenceiq.cloudbreak.converter;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.CredentialJson;
import com.sequenceiq.cloudbreak.controller.validation.AWSCredentialParam;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.service.credential.aws.AwsCredentialHandler;

@Component
public class AwsCredentialConverter extends AbstractConverter<CredentialJson, AwsCredential> {

    @Autowired
    private SnsTopicConverter snsTopicConverter;

    @Autowired
    private AwsCredentialHandler awsCredentialHandler;

    @Override
    public CredentialJson convert(AwsCredential entity) {
        CredentialJson credentialJson = new CredentialJson();
        credentialJson.setId(entity.getId());
        credentialJson.setCloudPlatform(CloudPlatform.AWS);
        credentialJson.setName(entity.getName());
        Map<String, Object> params = new HashMap<>();
        params.put(AWSCredentialParam.ROLE_ARN.getName(), entity.getRoleArn());
        params.put(AWSCredentialParam.SNS_TOPICS.getName(), snsTopicConverter.convertAllEntityToJson(entity.getSnsTopics()));
        credentialJson.setParameters(params);
        credentialJson.setDescription(entity.getDescription() == null ? "" : entity.getDescription());
        credentialJson.setPublicKey(entity.getPublicKey());
        return credentialJson;
    }

    @Override
    public AwsCredential convert(CredentialJson json) {
        AwsCredential awsCredential = new AwsCredential();
        awsCredential.setName(json.getName());
        awsCredential.setRoleArn(String.valueOf(json.getParameters().get(AWSCredentialParam.ROLE_ARN.getName())));
        awsCredential.setCloudPlatform(CloudPlatform.AWS);
        awsCredential.setDescription(json.getDescription());
        awsCredential.setPublicKey(json.getPublicKey());
        awsCredential = awsCredentialHandler.init(awsCredential);
        return awsCredential;
    }
}
