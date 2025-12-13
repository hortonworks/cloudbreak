package com.sequenceiq.redbeams.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CredentialTest {

    private static final String CRN = "crn";

    private static final String NAME = "name";

    private static final String ATTRIBUTES = "attributes";

    private static final String SUBSCRIPTION_ID = "subscriptionId";

    private Credential underTest;

    @BeforeEach
    public void setUp() {
        underTest = new Credential(CRN, NAME, ATTRIBUTES, "acc");
    }

    @Test
    void testGetters() {
        assertEquals(CRN, underTest.getCrn());
        assertEquals(NAME, underTest.getName());
        assertEquals(ATTRIBUTES, underTest.getAttributes());

        assertTrue(underTest.getAzure().isEmpty());
    }

    @Test
    void testAzureParameters() {
        Credential.AzureParameters azure = new Credential.AzureParameters(SUBSCRIPTION_ID);
        underTest = new Credential(CRN, NAME, ATTRIBUTES, azure, "acc");

        assertEquals(SUBSCRIPTION_ID, underTest.getAzure().get().getSubscriptionId());
    }
}
