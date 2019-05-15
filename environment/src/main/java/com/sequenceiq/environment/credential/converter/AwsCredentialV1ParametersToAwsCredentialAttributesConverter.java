package com.sequenceiq.environment.credential.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.api.credential.model.parameters.aws.AwsCredentialV1Parameters;
import com.sequenceiq.environment.api.credential.model.parameters.aws.KeyBasedCredentialParameters;
import com.sequenceiq.environment.api.credential.model.parameters.aws.RoleBasedCredentialParameters;
import com.sequenceiq.environment.credential.attributes.aws.AwsCredentialAttributes;
import com.sequenceiq.environment.credential.attributes.aws.KeyBasedCredentialAttributes;
import com.sequenceiq.environment.credential.attributes.aws.RoleBasedCredentialAttributes;

@Component
public class AwsCredentialV1ParametersToAwsCredentialAttributesConverter {

    public AwsCredentialAttributes convert(AwsCredentialV1Parameters source) {
        AwsCredentialAttributes response = new AwsCredentialAttributes();
        response.setKeyBased(getKeyBased(source.getKeyBased()));
        response.setRoleBased(getRoleBased(source.getRoleBased()));
        return response;
    }

    public AwsCredentialV1Parameters convert(AwsCredentialAttributes source) {
        AwsCredentialV1Parameters response = new AwsCredentialV1Parameters();
        response.setKeyBased(getKeyBased(source.getKeyBased()));
        response.setRoleBased(getRoleBased(source.getRoleBased()));
        return response;
    }

    private RoleBasedCredentialAttributes getRoleBased(RoleBasedCredentialParameters source) {
        RoleBasedCredentialAttributes roleBased = new RoleBasedCredentialAttributes();
        roleBased.setRoleArn(source.getRoleArn());
        return roleBased;
    }

    private KeyBasedCredentialAttributes getKeyBased(KeyBasedCredentialParameters source) {
        KeyBasedCredentialAttributes keyBased = new KeyBasedCredentialAttributes();
        keyBased.setAccessKey(source.getAccessKey());
        keyBased.setSecretKey(source.getSecretKey());
        return keyBased;
    }

    private RoleBasedCredentialParameters getRoleBased(RoleBasedCredentialAttributes source) {
        RoleBasedCredentialParameters roleBased = new RoleBasedCredentialParameters();
        roleBased.setRoleArn(source.getRoleArn());
        return roleBased;
    }

    private KeyBasedCredentialParameters getKeyBased(KeyBasedCredentialAttributes source) {
        KeyBasedCredentialParameters keyBased = new KeyBasedCredentialParameters();
        keyBased.setAccessKey(source.getAccessKey());
        keyBased.setSecretKey(source.getSecretKey());
        return keyBased;
    }
}
