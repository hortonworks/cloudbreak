package com.sequenceiq.it.cloudbreak;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

public class CredentialDeleteByNameTest extends AbstractCloudbreakIntegrationTest {
    @Test
    @Parameters("credentialName")
    public void testDeleteCredentialByName(String credentialName) {
        // GIVEN
        // WHEN
        getCloudbreakClient().credentialV4Endpoint().delete(1L, credentialName);
        // THEN no exception
    }
}
