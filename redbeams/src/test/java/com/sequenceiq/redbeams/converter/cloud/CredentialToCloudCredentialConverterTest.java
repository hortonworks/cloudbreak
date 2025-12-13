package com.sequenceiq.redbeams.converter.cloud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.redbeams.dto.Credential;

class CredentialToCloudCredentialConverterTest {

    private Credential credential;

    private CredentialToCloudCredentialConverter underTest;

    @BeforeEach
    public void setUp() throws Exception {
        underTest = new CredentialToCloudCredentialConverter();
    }

    @Test
    void testConvert() {
        credential = new Credential("userCrn", "userId", "{ \"foo\": \"bar\" }", "account");

        CloudCredential cloudCredential = underTest.convert(credential);

        assertEquals("userCrn", cloudCredential.getId());
        assertEquals("userId", cloudCredential.getName());
        assertEquals(1, cloudCredential.getParameters().size());
        assertEquals("bar", cloudCredential.getParameters().get("foo"));
    }

    @Test
    void testConvertNoAttributes() {
        credential = new Credential("userCrn", "userId", null, "account");

        CloudCredential cloudCredential = underTest.convert(credential);

        assertEquals("userCrn", cloudCredential.getId());
        assertEquals("userId", cloudCredential.getName());
        assertTrue(cloudCredential.getParameters().isEmpty());
    }

    @Test
    void testConvertNull() {
        assertNull(underTest.convert(null));
    }

}
