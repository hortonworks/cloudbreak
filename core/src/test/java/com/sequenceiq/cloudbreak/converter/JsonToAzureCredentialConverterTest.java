package com.sequenceiq.cloudbreak.converter;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.controller.json.CredentialRequest;
import com.sequenceiq.cloudbreak.domain.AzureCredential;

public class JsonToAzureCredentialConverterTest extends AbstractJsonConverterTest<CredentialRequest> {

    private JsonToAzureCredentialConverter underTest;

    @Before
    public void setUp() {
        underTest = new JsonToAzureCredentialConverter();
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        AzureCredential result = underTest.convert(getRequest("credential/azure-credential.json"));
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("cerFile", "jksFile", "sshCerFile"));
    }

    @Override
    public Class<CredentialRequest> getRequestClass() {
        return CredentialRequest.class;
    }
}
