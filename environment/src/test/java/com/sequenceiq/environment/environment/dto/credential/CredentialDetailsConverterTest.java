package com.sequenceiq.environment.environment.dto.credential;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
                mock(CloudPlatformAwareCredentialDetailsConverter.class);
        when(cloudPlatformAwareCredentialDetailsConverterMap.containsKey(any())).thenReturn(true);
        when(cloudPlatformAwareCredentialDetailsConverterMap.get(any())).thenReturn(cloudPlatformAwareCredentialDetailsConverter);
        CredentialDetails.Builder builder = CredentialDetails.builder();
        when(cloudPlatformAwareCredentialDetailsConverter.convertCredentialDetails(
                any(CredentialAttributes.class), any(CredentialDetails.Builder.class))).thenReturn(builder);
        CredentialDetails credentialDetails = underTest.credentialToCredentialDetails(CloudPlatform.AWS, credential);
        assertEquals(builder.build().getCredentialType(), credentialDetails.getCredentialType());
    }

    @Test
    void testCredentialToCredentialDetailsWhenCloudplatformNull() throws Exception {
        Credential credential = new Credential();
        CredentialAttributes credentialAttributes = new CredentialAttributes();
        credential.setAttributes(JsonUtil.writeValueAsString(credentialAttributes));
        CredentialDetails credentialDetails = underTest.credentialToCredentialDetails(null, credential);
        assertEquals(CredentialType.UNKNOWN, credentialDetails.getCredentialType());
    }

    @Test
    void testCredentialToCredentialDetailsWhenNoCloudPlatformAwareConverter() throws Exception {
        Credential credential = new Credential();
        CredentialAttributes credentialAttributes = new CredentialAttributes();
        credential.setAttributes(JsonUtil.writeValueAsString(credentialAttributes));
        CredentialDetails credentialDetails = underTest.credentialToCredentialDetails(CloudPlatform.AWS, credential);
        assertEquals(CredentialType.UNKNOWN, credentialDetails.getCredentialType());
    }

    @Test
    void testCredentialToCredentialDetailsWhenIllegalAttributesJson() throws Exception {
        Credential credential = new Credential();
        credential.setAttributes("Illegal json");
        CredentialDetails credentialDetails = underTest.credentialToCredentialDetails(CloudPlatform.AWS, credential);
        assertEquals(CredentialType.UNKNOWN, credentialDetails.getCredentialType());
    }
}
