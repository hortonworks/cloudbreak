package com.sequenceiq.it.cloudbreak.mock

import org.testng.Assert
import org.testng.annotations.Optional
import org.testng.annotations.Parameters
import org.testng.annotations.Test

import com.sequenceiq.cloudbreak.api.model.NetworkJson
import com.sequenceiq.it.cloudbreak.AbstractCloudbreakIntegrationTest
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants

class MockNetworkCreationTest : AbstractCloudbreakIntegrationTest() {
    @Test
    @Parameters("networkName", "subnetCIDR")
    @Throws(Exception::class)
    fun testGcpTemplateCreation(@Optional("it-mock-network") networkName: String, @Optional("10.0.36.0/24") subnetCIDR: String) {
        // GIVEN
        // WHEN
        // TODO: publicInAccount
        val networkJson = NetworkJson()
        networkJson.description = "Mock network for integration testing"
        networkJson.name = networkName
        networkJson.subnetCIDR = subnetCIDR
        networkJson.cloudPlatform = "MOCK"

        val id = cloudbreakClient.networkEndpoint().postPrivate(networkJson).id!!.toString()
        // THEN
        Assert.assertNotNull(id)
        itContext.putContextParam(CloudbreakITContextConstants.NETWORK_ID, id, true)
    }
}
