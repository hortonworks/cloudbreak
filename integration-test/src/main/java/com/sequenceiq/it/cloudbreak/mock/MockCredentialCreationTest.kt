package com.sequenceiq.it.cloudbreak.mock

import java.util.HashMap
import java.util.UUID

import org.springframework.beans.factory.annotation.Value
import org.springframework.util.StringUtils
import org.testng.Assert
import org.testng.annotations.Optional
import org.testng.annotations.Parameters
import org.testng.annotations.Test

import com.sequenceiq.cloudbreak.api.model.CredentialRequest
import com.sequenceiq.it.cloudbreak.AbstractCloudbreakIntegrationTest
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants
import com.sequenceiq.it.util.ResourceUtil

class MockCredentialCreationTest : AbstractCloudbreakIntegrationTest() {
    @Value("${integrationtest.mockcredential.name}")
    private val defaultName: String? = null
    @Value("${integrationtest.mockcredential.publicKeyFile}")
    private val defaultPublicKeyFile: String? = null

    @Test
    @Parameters("credentialName", "publicKeyFile")
    @Throws(Exception::class)
    fun testMockCredentialCreation(@Optional("") credentialName: String, @Optional("") publicKeyFile: String) {
        var credentialName = credentialName
        var publicKeyFile = publicKeyFile
        // GIVEN
        credentialName = if (StringUtils.hasLength(credentialName)) credentialName else defaultName

        val credentialRequest = CredentialRequest()
        credentialRequest.name = credentialName + UUID.randomUUID()
        publicKeyFile = if (StringUtils.hasLength(publicKeyFile)) publicKeyFile else defaultPublicKeyFile
        val publicKey = ResourceUtil.readStringFromResource(applicationContext, publicKeyFile).replace("\n".toRegex(), "")
        credentialRequest.publicKey = publicKey
        credentialRequest.description = "Mock Rm credential for integrationtest"
        val map = HashMap<String, Any>()
        map.put("keystoneVersion", "cb-keystone-v2")
        map.put("selector", "cb-keystone-v2")

        credentialRequest.parameters = map
        credentialRequest.cloudPlatform = "MOCK"
        // WHEN
        // TODO publicInAccount
        val id = cloudbreakClient.credentialEndpoint().postPrivate(credentialRequest).id!!.toString()
        // THEN
        Assert.assertNotNull(id)
        itContext.putContextParam(CloudbreakITContextConstants.CREDENTIAL_ID, id, true)
    }
}
