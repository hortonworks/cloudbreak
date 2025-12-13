package com.sequenceiq.environment.environment.dto.credential;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.credential.CredentialDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.credential.CredentialType;
import com.sequenceiq.environment.credential.attributes.CredentialAttributes;
import com.sequenceiq.environment.credential.attributes.aws.AwsCredentialAttributes;
import com.sequenceiq.environment.credential.attributes.aws.KeyBasedCredentialAttributes;
import com.sequenceiq.environment.credential.attributes.aws.RoleBasedCredentialAttributes;

@ExtendWith(MockitoExtension.class)
public class AwsCredentialDetailsConverterTest {
    @InjectMocks
    private AwsCredentialDetailsConverter underTest;

    @Test
    void convertCredentialDetailsKeyBased() {
        CredentialAttributes credentialAttributes = new CredentialAttributes();
        AwsCredentialAttributes awsCredentialAttributes = new AwsCredentialAttributes();
        awsCredentialAttributes.setKeyBased(new KeyBasedCredentialAttributes());
        credentialAttributes.setAws(awsCredentialAttributes);
        CredentialDetails.Builder builder = underTest.convertCredentialDetails(credentialAttributes, CredentialDetails.builder());
        assertEquals(CredentialType.AWS_KEY_BASED, builder.build().getCredentialType());
    }

    @Test
    void convertCredentialDetailsRoleBased() {
        CredentialAttributes credentialAttributes = new CredentialAttributes();
        AwsCredentialAttributes awsCredentialAttributes = new AwsCredentialAttributes();
        awsCredentialAttributes.setRoleBased(new RoleBasedCredentialAttributes());
        credentialAttributes.setAws(awsCredentialAttributes);
        CredentialDetails.Builder builder = underTest.convertCredentialDetails(credentialAttributes, CredentialDetails.builder());
        assertEquals(CredentialType.AWS_ROLE_BASED, builder.build().getCredentialType());
    }

    @Test
    void convertCredentialDetailsWhenAttributesNull() {
        CredentialAttributes credentialAttributes = new CredentialAttributes();
        CredentialDetails.Builder builder = underTest.convertCredentialDetails(credentialAttributes, CredentialDetails.builder());
        assertEquals(CredentialType.UNKNOWN, builder.build().getCredentialType());
    }
}
