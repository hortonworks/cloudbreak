package com.sequenceiq.consumption.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.consumption.dto.Credential;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class CredentialToCloudCredentialConverterTest {

    private static final String PLATFORM = "platform";

    private static final String NAME = "name";

    private static final String CRN = "userCrn";

    private static final String ACCOUNT_ID = "accountId";

    private Credential credential;

    private CredentialToCloudCredentialConverter underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new CredentialToCloudCredentialConverter();
    }

    @Test
    public void testConvert() {
        credential = new Credential(PLATFORM, NAME, "{ \"foo\": \"bar\" }", CRN, ACCOUNT_ID);

        CloudCredential cloudCredential = underTest.convert(credential);

        assertEquals(CRN, cloudCredential.getId());
        assertEquals(NAME, cloudCredential.getName());
        assertEquals(1, cloudCredential.getParameters().size());
        assertEquals("bar", cloudCredential.getParameters().get("foo"));
        assertEquals(ACCOUNT_ID, cloudCredential.getAccountId());
    }

    @Test
    public void testConvertNoAttributes() {
        credential = new Credential(PLATFORM, NAME, null, CRN, ACCOUNT_ID);

        CloudCredential cloudCredential = underTest.convert(credential);

        assertEquals(CRN, cloudCredential.getId());
        assertEquals(NAME, cloudCredential.getName());
        assertTrue(cloudCredential.getParameters().isEmpty());
        assertEquals(ACCOUNT_ID, cloudCredential.getAccountId());
    }

    @SuppressFBWarnings(value = "NP", justification = "Converter may be passed a null")
    @Test
    public void testConvertNull() {
        assertNull(underTest.convert(null));
    }
}
