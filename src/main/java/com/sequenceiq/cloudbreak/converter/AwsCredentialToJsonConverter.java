package com.sequenceiq.cloudbreak.converter;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.CredentialJson;
import com.sequenceiq.cloudbreak.controller.validation.AWSCredentialParam;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;

@Component
public class AwsCredentialToJsonConverter extends AbstractConversionServiceAwareConverter<AwsCredential, CredentialJson> {
    @Override
    public CredentialJson convert(AwsCredential source) {
        CredentialJson credentialJson = new CredentialJson();
        credentialJson.setId(source.getId());
        credentialJson.setCloudPlatform(CloudPlatform.AWS);
        credentialJson.setName(source.getName());
        credentialJson.setPublicInAccount(source.isPublicInAccount());
        Map<String, Object> params = new HashMap<>();
        params.put(AWSCredentialParam.ROLE_ARN.getName(), source.getRoleArn());
        credentialJson.setParameters(params);
        credentialJson.setDescription(source.getDescription() == null ? "" : source.getDescription());
        credentialJson.setPublicKey(source.getPublicKey());
        return credentialJson;
    }
}
