package com.sequenceiq.consumption.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@ExtendWith(MockitoExtension.class)
class CredentialResponseToCloudCredentialConverterTest {

    private static final String PLATFORM = "platform";

    private static final String NAME = "name";

    private static final String CRN = "userCrn";

    private static final String ACCOUNT_ID = "accountId";

    @Mock
    private SecretService secretService;

    @InjectMocks
    private CredentialResponseToCloudCredentialConverter underTest;

    @Test
    void testConvert() {
        when(secretService.getByResponse(any())).thenReturn("{ \"foo\": \"bar\" }");
        CredentialResponse credentialResponse = createCredentialResponse(new SecretResponse("path", "secretPath"));

        CloudCredential cloudCredential = underTest.convert(credentialResponse);

        assertEquals(CRN, cloudCredential.getId());
        assertEquals(NAME, cloudCredential.getName());
        assertEquals(1, cloudCredential.getParameters().size());
        assertEquals("bar", cloudCredential.getParameters().get("foo"));
        assertEquals(ACCOUNT_ID, cloudCredential.getAccountId());
    }

    @Test
    void testConvertNoAttributes() {
        when(secretService.getByResponse(any())).thenReturn(null);
        CredentialResponse credentialResponse = createCredentialResponse(null);

        CloudCredential cloudCredential = underTest.convert(credentialResponse);

        assertEquals(CRN, cloudCredential.getId());
        assertEquals(NAME, cloudCredential.getName());
        assertTrue(cloudCredential.getParameters().isEmpty());
        assertEquals(ACCOUNT_ID, cloudCredential.getAccountId());
    }

    @SuppressFBWarnings(value = "NP", justification = "Converter may be passed a null")
    @Test
    void testConvertNull() {
        assertNull(underTest.convert(null));
    }

    private CredentialResponse createCredentialResponse(SecretResponse attributes) {
        CredentialResponse credentialResponse = new CredentialResponse();
        credentialResponse.setCrn(CRN);
        credentialResponse.setName(NAME);
        credentialResponse.setAttributes(attributes);
        credentialResponse.setAccountId(ACCOUNT_ID);
        credentialResponse.setCloudPlatform(PLATFORM);
        credentialResponse.setSkipOrgPolicyDecisions(true);
        return credentialResponse;
    }
}
