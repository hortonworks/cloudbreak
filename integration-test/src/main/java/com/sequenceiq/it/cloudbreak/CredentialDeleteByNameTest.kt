package com.sequenceiq.it.cloudbreak

import org.testng.annotations.Parameters
import org.testng.annotations.Test

class CredentialDeleteByNameTest : AbstractCloudbreakIntegrationTest() {
    @Test
    @Parameters("credentialName")
    @Throws(Exception::class)
    fun testDeleteCredentialByName(credentialName: String) {
        // GIVEN
        // WHEN
        cloudbreakClient.credentialEndpoint().deletePublic(credentialName)
        // THEN no exception
    }
}
