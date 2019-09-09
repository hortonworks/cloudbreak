package com.sequenceiq.redbeams.converter.cloud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.redbeams.dto.Credential;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.junit.Before;
import org.junit.Test;

public class CredentialToCloudCredentialConverterTest {

    private Credential credential;

    private CredentialToCloudCredentialConverter underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new CredentialToCloudCredentialConverter();
    }

    @Test
    public void testConvert() {
        credential = new Credential("userCrn", "userId", "{ \"foo\": \"bar\" }");

        CloudCredential cloudCredential = underTest.convert(credential);

        assertEquals("userCrn", cloudCredential.getId());
        assertEquals("userId", cloudCredential.getName());
        assertEquals(1, cloudCredential.getParameters().size());
        assertEquals("bar", cloudCredential.getParameters().get("foo"));
    }

    @Test
    public void testConvertNoAttributes() {
        credential = new Credential("userCrn", "userId", null);

        CloudCredential cloudCredential = underTest.convert(credential);

        assertEquals("userCrn", cloudCredential.getId());
        assertEquals("userId", cloudCredential.getName());
        assertTrue(cloudCredential.getParameters().isEmpty());
    }

    @SuppressFBWarnings(value = "NP", justification = "Converter may be passed a null")
    @Test
    public void testConvertNull() {
        assertNull(underTest.convert(null));
    }

}
