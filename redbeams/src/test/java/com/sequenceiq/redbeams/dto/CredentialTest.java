package com.sequenceiq.redbeams.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class CredentialTest {

    private static final String CRN = "crn";

    private static final String NAME = "name";

    private static final String ATTRIBUTES = "attributes";

    private static final String SUBSCRIPTION_ID = "subscriptionId";

    private Credential underTest;

    @Before
    public void setUp() {
        underTest = new Credential(CRN, NAME, ATTRIBUTES);
    }

    @Test
    public void testGetters() {
        assertEquals(CRN, underTest.getCrn());
        assertEquals(NAME, underTest.getName());
        assertEquals(ATTRIBUTES, underTest.getAttributes());

        assertTrue(underTest.getAzure().isEmpty());
    }

    @Test
    public void testAzureParameters() {
        Credential.AzureParameters azure = new Credential.AzureParameters(SUBSCRIPTION_ID);
        underTest = new Credential(CRN, NAME, ATTRIBUTES, azure);

        assertEquals(SUBSCRIPTION_ID, underTest.getAzure().get().getSubscriptionId());
    }
}
