package com.sequenceiq.cloudbreak.converter;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;

import org.jasypt.encryption.pbe.PBEStringCleanablePasswordEncryptor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.controller.json.CredentialRequest;
import com.sequenceiq.cloudbreak.domain.OpenStackCredential;

public class JsonToOpenStackCredentialConverterTest extends AbstractJsonConverterTest<CredentialRequest> {

    @InjectMocks
    private JsonToOpenStackCredentialConverter underTest;

    @Mock
    private PBEStringCleanablePasswordEncryptor encryptor;

    @Before
    public void setUp() {
        underTest = new JsonToOpenStackCredentialConverter();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testConvert() {
        // GIVEN
        given(encryptor.encrypt(anyString())).willReturn("encryptedString");
        // WHEN
        OpenStackCredential result = underTest.convert(getRequest("credential/openstack-credential.json"));
        // THEN
        assertAllFieldsNotNull(result);
    }

    @Override
    public Class<CredentialRequest> getRequestClass() {
        return CredentialRequest.class;
    }
}
