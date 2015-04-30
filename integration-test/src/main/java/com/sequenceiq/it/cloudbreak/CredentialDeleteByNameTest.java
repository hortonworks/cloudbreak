package com.sequenceiq.it.cloudbreak;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

public class CredentialDeleteByNameTest extends AbstractCloudbreakIntegrationTest {
    @Test
    @Parameters({ "credentialName" })
    public void testDeleteCredentialByName(String credentialName) throws Exception {
        // GIVEN
        // WHEN
        getClient().deleteCredentialByName(credentialName);
        // THEN no exception
    }
}
