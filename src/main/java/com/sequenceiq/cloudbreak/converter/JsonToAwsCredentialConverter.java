package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.CredentialJson;
import com.sequenceiq.cloudbreak.controller.validation.AWSCredentialParam;
import com.sequenceiq.cloudbreak.domain.AwsCredential;

@Component
public class JsonToAwsCredentialConverter extends AbstractConversionServiceAwareConverter<CredentialJson, AwsCredential> {
    @Override
    public AwsCredential convert(CredentialJson source) {
        AwsCredential awsCredential = new AwsCredential();
        awsCredential.setName(source.getName());
        awsCredential.setRoleArn(String.valueOf(source.getParameters().get(AWSCredentialParam.ROLE_ARN.getName())));
        awsCredential.setDescription(source.getDescription());
        awsCredential.setPublicKey(source.getPublicKey());
        return awsCredential;
    }
}
