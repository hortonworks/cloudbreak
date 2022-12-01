package com.sequenceiq.environment.environment.dto.credential;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.credential.CredentialDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.credential.CredentialType;
import com.sequenceiq.environment.credential.attributes.CredentialAttributes;
import com.sequenceiq.environment.credential.domain.Credential;

@ExtendWith(MockitoExtension.class)
public class CredentialDetailsConverterTest {
    @Mock
    private Map<CloudPlatform, CloudPlatformAwareCredentialDetailsConverter> cloudPlatformAwareCredentialDetailsConverterMap;

    @InjectMocks
    private CredentialDetailsConverter underTest;

    @Test
    void testCredentialToCredentialDetails() throws Exception {
        Credential credential = new Credential();
        CredentialAttributes credentialAttributes = new CredentialAttributes();
        credential.setAttributes(JsonUtil.writeValueAsString(credentialAttributes));
        CloudPlatformAwareCredentialDetailsConverter cloudPlatformAwareCredentialDetailsConverter =
                Mockito.mock(CloudPlatformAwareCredentialDetailsConverter.class);
        Mockito.when(cloudPlatformAwareCredentialDetailsConverterMap.containsKey(Mockito.any())).thenReturn(true);
        Mockito.when(cloudPlatformAwareCredentialDetailsConverterMap.get(Mockito.any())).thenReturn(cloudPlatformAwareCredentialDetailsConverter);
        CredentialDetails.Builder builder = CredentialDetails.builder();
        Mockito.when(cloudPlatformAwareCredentialDetailsConverter.convertCredentialDetails(
                Mockito.any(CredentialAttributes.class), Mockito.any(CredentialDetails.Builder.class))).thenReturn(builder);
        CredentialDetails credentialDetails = underTest.credentialToCredentialDetails(CloudPlatform.AWS, credential);
        Assertions.assertEquals(builder.build().getCredentialType(), credentialDetails.getCredentialType());
    }

    @Test
    void testCredentialToCredentialDetailsWhenCloudplatformNull() throws Exception {
        Credential credential = new Credential();
        CredentialAttributes credentialAttributes = new CredentialAttributes();
        credential.setAttributes(JsonUtil.writeValueAsString(credentialAttributes));
        CredentialDetails credentialDetails = underTest.credentialToCredentialDetails(null, credential);
        Assertions.assertEquals(CredentialType.UNKNOWN, credentialDetails.getCredentialType());
    }

    @Test
    void testCredentialToCredentialDetailsWhenNoCloudPlatformAwareConverter() throws Exception {
        Credential credential = new Credential();
        CredentialAttributes credentialAttributes = new CredentialAttributes();
        credential.setAttributes(JsonUtil.writeValueAsString(credentialAttributes));
        CredentialDetails credentialDetails = underTest.credentialToCredentialDetails(CloudPlatform.AWS, credential);
        Assertions.assertEquals(CredentialType.UNKNOWN, credentialDetails.getCredentialType());
    }

    @Test
    void testCredentialToCredentialDetailsWhenIllegalAttributesJson() throws Exception {
        Credential credential = new Credential();
        credential.setAttributes("Illegal json");
        CredentialDetails credentialDetails = underTest.credentialToCredentialDetails(CloudPlatform.AWS, credential);
        Assertions.assertEquals(CredentialType.UNKNOWN, credentialDetails.getCredentialType());
    }
}
