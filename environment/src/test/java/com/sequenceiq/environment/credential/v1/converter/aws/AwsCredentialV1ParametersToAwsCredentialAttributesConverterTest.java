package com.sequenceiq.environment.credential.v1.converter.aws;

import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.AwsCredentialParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.KeyBasedParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.RoleBasedParameters;
import com.sequenceiq.environment.credential.attributes.CredentialAttributes;
import com.sequenceiq.environment.credential.attributes.aws.AwsCredentialAttributes;
import com.sequenceiq.environment.credential.attributes.aws.RoleBasedCredentialAttributes;
import com.sequenceiq.environment.credential.domain.Credential;

class AwsCredentialV1ParametersToAwsCredentialAttributesConverterTest {

    private AwsCredentialV1ParametersToAwsCredentialAttributesConverter underTest;

    @BeforeEach
    void setUp() {
        underTest = new AwsCredentialV1ParametersToAwsCredentialAttributesConverter();
    }

    @Test
    void convertRoleBasedWhenItIsNotUpdate() {
        RoleBasedParameters roleBasedParameters = new RoleBasedParameters();
        roleBasedParameters.setRoleArn("roleArn");
        AwsCredentialParameters source = new AwsCredentialParameters();
        source.setRoleBased(roleBasedParameters);
        source.setDefaultRegion("us-east-1");

        AwsCredentialAttributes actual = underTest.convert(source, Optional.empty());

        Assertions.assertNotNull(actual);
        Assertions.assertNull(actual.getDefaultRegion());
        Assertions.assertEquals(roleBasedParameters.getRoleArn(), actual.getRoleBased().getRoleArn());
    }

    @Test
    void convertKeyBasedWhenItIsNotUpdate() {
        KeyBasedParameters keyBasedParameters = new KeyBasedParameters();
        keyBasedParameters.setAccessKey("accessKey");
        keyBasedParameters.setSecretKey("secretKey");
        AwsCredentialParameters source = new AwsCredentialParameters();
        source.setKeyBased(keyBasedParameters);
        source.setDefaultRegion("us-east-1");

        AwsCredentialAttributes actual = underTest.convert(source, Optional.empty());

        Assertions.assertNotNull(actual);
        Assertions.assertNull(actual.getDefaultRegion());
        Assertions.assertEquals(keyBasedParameters.getAccessKey(), actual.getKeyBased().getAccessKey());
        Assertions.assertEquals(keyBasedParameters.getSecretKey(), actual.getKeyBased().getSecretKey());
    }

    @Test
    void convertRoleBasedWhenItIsUpdateAndDefaultRegionIsNotSet() {
        RoleBasedParameters roleBasedParameters = new RoleBasedParameters();
        roleBasedParameters.setRoleArn("roleArn");
        AwsCredentialParameters source = new AwsCredentialParameters();
        source.setRoleBased(roleBasedParameters);
        Credential credential = getRoleBasedCredential(null);

        AwsCredentialAttributes actual = underTest.convert(source, Optional.of(credential));

        Assertions.assertNotNull(actual);
        Assertions.assertNull(actual.getDefaultRegion());
        Assertions.assertEquals(roleBasedParameters.getRoleArn(), actual.getRoleBased().getRoleArn());
    }

    @ParameterizedTest
    @ValueSource(strings = { "defaultRegion", "otherDefaultRegion", "" })
    void convertRoleBasedWhenItIsUpdateAndDefaultRegionIsSet(String originalDefaultRegion) {
        RoleBasedParameters roleBasedParameters = new RoleBasedParameters();
        roleBasedParameters.setRoleArn("roleArn");
        AwsCredentialParameters source = new AwsCredentialParameters();
        source.setRoleBased(roleBasedParameters);
        source.setDefaultRegion("defaultRegion");
        Credential credential = getRoleBasedCredential(originalDefaultRegion);

        AwsCredentialAttributes actual = underTest.convert(source, Optional.of(credential));

        Assertions.assertNotNull(actual);
        Assertions.assertEquals(roleBasedParameters.getRoleArn(), actual.getRoleBased().getRoleArn());
        Assertions.assertEquals(source.getDefaultRegion(), actual.getDefaultRegion());
    }

    @NotNull
    private static Credential getRoleBasedCredential(String originalDefaultRegion) {
        RoleBasedCredentialAttributes roleBasedCredentialAttributes = new RoleBasedCredentialAttributes();
        roleBasedCredentialAttributes.setRoleArn("oldRoleArn");
        AwsCredentialAttributes awsCredentialAttributes = new AwsCredentialAttributes();
        awsCredentialAttributes.setRoleBased(roleBasedCredentialAttributes);
        awsCredentialAttributes.setDefaultRegion(originalDefaultRegion);
        CredentialAttributes credentialAttributes = new CredentialAttributes();
        credentialAttributes.setAws(awsCredentialAttributes);
        Credential credential = new Credential();
        credential.setAttributes(JsonUtil.writeValueAsStringUnchecked(credentialAttributes));
        return credential;
    }
}