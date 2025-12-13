package com.sequenceiq.environment.credential.v1.converter.aws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Optional;

import org.jetbrains.annotations.NotNull;
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

        assertNotNull(actual);
        assertNull(actual.getDefaultRegion());
        assertEquals(roleBasedParameters.getRoleArn(), actual.getRoleBased().getRoleArn());
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

        assertNotNull(actual);
        assertNull(actual.getDefaultRegion());
        assertEquals(keyBasedParameters.getAccessKey(), actual.getKeyBased().getAccessKey());
        assertEquals(keyBasedParameters.getSecretKey(), actual.getKeyBased().getSecretKey());
    }

    @Test
    void convertRoleBasedWhenItIsUpdateAndDefaultRegionIsNotSet() {
        RoleBasedParameters roleBasedParameters = new RoleBasedParameters();
        roleBasedParameters.setRoleArn("roleArn");
        AwsCredentialParameters source = new AwsCredentialParameters();
        source.setRoleBased(roleBasedParameters);
        Credential credential = getRoleBasedCredential(null);

        AwsCredentialAttributes actual = underTest.convert(source, Optional.of(credential));

        assertNotNull(actual);
        assertNull(actual.getDefaultRegion());
        assertEquals(roleBasedParameters.getRoleArn(), actual.getRoleBased().getRoleArn());
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

        assertNotNull(actual);
        assertEquals(roleBasedParameters.getRoleArn(), actual.getRoleBased().getRoleArn());
        assertEquals(source.getDefaultRegion(), actual.getDefaultRegion());
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