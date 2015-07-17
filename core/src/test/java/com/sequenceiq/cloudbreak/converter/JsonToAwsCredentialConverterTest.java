package com.sequenceiq.cloudbreak.converter;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.controller.json.CredentialRequest;
import com.sequenceiq.cloudbreak.domain.AwsCredential;

public class JsonToAwsCredentialConverterTest extends AbstractJsonConverterTest<CredentialRequest> {

    private JsonToAwsCredentialConverter underTest;

    @Before
    public void setUp() {
        underTest = new JsonToAwsCredentialConverter();
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        AwsCredential result = underTest.convert(getRequest("credential/aws-credential.json"));
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("keyPairName", "temporaryAwsCredentials"));
    }

    @Override
    public Class<CredentialRequest> getRequestClass() {
        return CredentialRequest.class;
    }
}
