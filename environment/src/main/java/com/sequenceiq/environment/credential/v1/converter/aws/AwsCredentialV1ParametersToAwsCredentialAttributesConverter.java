package com.sequenceiq.environment.credential.v1.converter.aws;

import static com.sequenceiq.cloudbreak.util.NullUtil.doIfNotNull;

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
        AwsCredentialAttributes response = new AwsCredentialAttributes();
        doIfNotNull(source.getKeyBased(), params -> response.setKeyBased(getKeyBased(params)));
        doIfNotNull(source.getKeyBased(), params -> response.setKeyBased(getKeyBased(params)));
        doIfNotNull(source.getRoleBased(), params -> response.setRoleBased(getRoleBased(params)));
        return response;
    }

    public AwsCredentialParameters convert(AwsCredentialAttributes source) {
        AwsCredentialParameters response = new AwsCredentialParameters();
        doIfNotNull(source.getKeyBased(), params -> response.setKeyBased(getKeyBased(params)));
        doIfNotNull(source.getRoleBased(), params -> response.setRoleBased(getRoleBased(params)));
        return response;
    }

    private RoleBasedCredentialAttributes getRoleBased(RoleBasedParameters source) {
        RoleBasedCredentialAttributes roleBased = new RoleBasedCredentialAttributes();
        roleBased.setRoleArn(source.getRoleArn());
        return roleBased;
    }

    private KeyBasedCredentialAttributes getKeyBased(KeyBasedParameters source) {
        KeyBasedCredentialAttributes keyBased = new KeyBasedCredentialAttributes();
        keyBased.setAccessKey(source.getAccessKey());
        keyBased.setSecretKey(source.getSecretKey());
        return keyBased;
    }

    private RoleBasedParameters getRoleBased(RoleBasedCredentialAttributes source) {
        RoleBasedParameters roleBased = new RoleBasedParameters();
        roleBased.setRoleArn(source.getRoleArn());
        return roleBased;
    }

    private KeyBasedParameters getKeyBased(KeyBasedCredentialAttributes source) {
        KeyBasedParameters keyBased = new KeyBasedParameters();
        keyBased.setAccessKey(source.getAccessKey());
        keyBased.setSecretKey(source.getSecretKey());
        return keyBased;
    }
}
