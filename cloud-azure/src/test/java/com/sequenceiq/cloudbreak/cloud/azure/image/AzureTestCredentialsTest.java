package com.sequenceiq.cloudbreak.cloud.azure.image;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class AzureTestCredentialsTest {

    private AzureTestCredentials underTest = new AzureTestCredentials();

    @Test
    public void testCredentialIsFilledIn() {
        assertNotNull(underTest.getCredentials());
    }

    @Test
    public void testGetStorageAccountStringNotNull() {
        assertNotNull(underTest.getStorageAccountConnectionString());
        assertTrue(underTest.getStorageAccountConnectionString().contains(";"));
    }

}
