package com.sequenceiq.it.cloudbreak

import java.util.HashMap

import org.springframework.beans.factory.annotation.Value
import org.springframework.util.StringUtils
import org.testng.Assert
import org.testng.annotations.Optional
import org.testng.annotations.Parameters
import org.testng.annotations.Test

import com.sequenceiq.cloudbreak.api.model.NetworkJson

class OpenStackNetworkCreationTest : AbstractCloudbreakIntegrationTest() {
    @Value("${integrationtest.openstack.publicNetId}")
    private val defaultPublicNetId: String? = null

    @Test
    @Parameters("networkName", "subnetCIDR", "publicNetId")
    @Throws(Exception::class)
    fun testOpenstackNetworkCreation(@Optional("it-openstack-network") networkName: String, @Optional("10.0.36.0/24") subnetCIDR: String,
                                     @Optional("") publicNetId: String) {
        var publicNetId = publicNetId
        // GIVEN
        publicNetId = getPublicNetId(publicNetId, defaultPublicNetId)
        // WHEN
        // TODO: publicInAccount
        val networkJson = NetworkJson()
        networkJson.description = "OpenStack network for integration testing"
        networkJson.name = networkName
        networkJson.subnetCIDR = subnetCIDR
        val map = HashMap<String, Any>()
        map.put("publicNetId", publicNetId)
        networkJson.parameters = map
        networkJson.cloudPlatform = "OPENSTACK"

        val id = cloudbreakClient.networkEndpoint().postPrivate(networkJson).id!!.toString()
        // THEN
        Assert.assertNotNull(id)
        itContext.putContextParam(CloudbreakITContextConstants.NETWORK_ID, id, true)
    }

    private fun getPublicNetId(publicNetId: String, defaultPublicNetId: String): String {
        var publicNetId = publicNetId
        if ("__empty__" == publicNetId) {
            publicNetId = ""
        } else if (StringUtils.isEmpty(publicNetId)) {
            publicNetId = defaultPublicNetId
        }
        return publicNetId
    }
}
