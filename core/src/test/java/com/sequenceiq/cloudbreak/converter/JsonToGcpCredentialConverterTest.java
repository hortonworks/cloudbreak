package com.sequenceiq.cloudbreak.converter;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.controller.json.CredentialRequest;
import com.sequenceiq.cloudbreak.domain.GcpCredential;

public class JsonToGcpCredentialConverterTest extends AbstractJsonConverterTest<CredentialRequest> {

    private JsonToGcpCredentialConverter underTest;

    @Before
    public void setUp() {
        underTest = new JsonToGcpCredentialConverter();
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        GcpCredential result = underTest.convert(getRequest("credential/gcp-credential.json"));
        // THEN
        assertAllFieldsNotNull(result);
    }

    @Override
    public Class<CredentialRequest> getRequestClass() {
        return CredentialRequest.class;
    }
}
