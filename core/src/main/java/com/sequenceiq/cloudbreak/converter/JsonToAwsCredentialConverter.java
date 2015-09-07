package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.json.CredentialRequest;
import com.sequenceiq.cloudbreak.controller.validation.AWSCredentialParam;
import com.sequenceiq.cloudbreak.domain.AwsCredential;

@Component
public class JsonToAwsCredentialConverter extends AbstractConversionServiceAwareConverter<CredentialRequest, AwsCredential> {

    private static final String DEFAULT_SSH_USER = "ec2-user";

    @Override
    public AwsCredential convert(CredentialRequest source) {
        AwsCredential awsCredential = new AwsCredential();
        awsCredential.setName(source.getName());
        awsCredential.setRoleArn(String.valueOf(source.getParameters().get(AWSCredentialParam.ROLE_ARN.getName())));
        awsCredential.setDescription(source.getDescription());
        awsCredential.setPublicKey(source.getPublicKey());
        if (source.getLoginUserName() != null) {
            throw new BadRequestException("You can not modify the ec2 user!");
        }
        awsCredential.setLoginUserName(DEFAULT_SSH_USER);
        return awsCredential;
    }
}
