package com.sequenceiq.environment.credential.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.AwsCredentialParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.KeyBasedParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.RoleBasedParameters;
import com.sequenceiq.environment.credential.attributes.aws.AwsCredentialAttributes;
import com.sequenceiq.environment.credential.attributes.aws.KeyBasedCredentialAttributes;
import com.sequenceiq.environment.credential.attributes.aws.RoleBasedCredentialAttributes;

@Component
public class AwsCredentialV1ParametersToAwsCredentialAttributesConverter {

    public AwsCredentialAttributes convert(AwsCredentialParameters source) {
        if (source == null) {
            return null;
        }
        AwsCredentialAttributes response = new AwsCredentialAttributes();
        response.setKeyBased(getKeyBased(source.getKeyBased()));
        response.setRoleBased(getRoleBased(source.getRoleBased()));
        return response;
    }

    public AwsCredentialParameters convert(AwsCredentialAttributes source) {
        if (source == null) {
            return null;
        }
        AwsCredentialParameters response = new AwsCredentialParameters();
        response.setKeyBased(getKeyBased(source.getKeyBased()));
        response.setRoleBased(getRoleBased(source.getRoleBased()));
        return response;
    }

    private RoleBasedCredentialAttributes getRoleBased(RoleBasedParameters source) {
        if (source == null) {
            return null;
        }
        RoleBasedCredentialAttributes roleBased = new RoleBasedCredentialAttributes();
        roleBased.setRoleArn(source.getRoleArn());
        return roleBased;
    }

    private KeyBasedCredentialAttributes getKeyBased(KeyBasedParameters source) {
        if (source == null) {
            return null;
        }
        KeyBasedCredentialAttributes keyBased = new KeyBasedCredentialAttributes();
        keyBased.setAccessKey(source.getAccessKey());
        keyBased.setSecretKey(source.getSecretKey());
        return keyBased;
    }

    private RoleBasedParameters getRoleBased(RoleBasedCredentialAttributes source) {
        if (source == null) {
            return null;
        }
        RoleBasedParameters roleBased = new RoleBasedParameters();
        roleBased.setRoleArn(source.getRoleArn());
        return roleBased;
    }

    private KeyBasedParameters getKeyBased(KeyBasedCredentialAttributes source) {
        if (source == null) {
            return null;
        }
        KeyBasedParameters keyBased = new KeyBasedParameters();
        keyBased.setAccessKey(source.getAccessKey());
        keyBased.setSecretKey(source.getSecretKey());
        return keyBased;
    }
}
