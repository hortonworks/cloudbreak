package com.sequenceiq.it.cloudbreak

import org.testng.annotations.Parameters
import org.testng.annotations.Test

class NetworkDeleteByNameTest : AbstractCloudbreakIntegrationTest() {
    @Test
    @Parameters("networkName")
    @Throws(Exception::class)
    fun testDeleteTemplateByName(networkName: String) {
        // GIVEN
        // WHEN
        cloudbreakClient.networkEndpoint().deletePublic(networkName)
        // THEN no exception
    }
}
