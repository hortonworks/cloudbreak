package com.sequenceiq.it.cloudbreak

import java.util.HashMap

import org.springframework.beans.factory.annotation.Value
import org.springframework.util.StringUtils
import org.testng.Assert
import org.testng.annotations.Optional
import org.testng.annotations.Parameters
import org.testng.annotations.Test

import com.sequenceiq.cloudbreak.api.model.CredentialRequest
import com.sequenceiq.it.util.ResourceUtil

class AzureRmCredentialCreationTest : AbstractCloudbreakIntegrationTest() {
    @Value("${integrationtest.azurermcredential.name}")
    private val defaultName: String? = null
    @Value("${integrationtest.azurermcredential.subscriptionId}")
    private val defaultSubscriptionId: String? = null
    @Value("${integrationtest.azurermcredential.secretKey}")
    private val defaultSecretKey: String? = null
    @Value("${integrationtest.azurermcredential.accessKey}")
    private val defaultAccesKey: String? = null
    @Value("${integrationtest.azurermcredential.tenantId}")
    private val defaultTenantId: String? = null
    @Value("${integrationtest.azurermcredential.publicKeyFile}")
    private val defaultPublicKeyFile: String? = null

    @Test
    @Parameters("credentialName", "subscriptionId", "secretKey", "accessKey", "tenantId", "publicKeyFile")
    @Throws(Exception::class)
    fun testAzureRMCredentialCreation(@Optional("itazurermcreden") credentialName: String, @Optional("") subscriptionId: String,
                                      @Optional("") secretKey: String, @Optional("") accessKey: String, @Optional("") tenantId: String,
                                      @Optional("") publicKeyFile: String) {
        var credentialName = credentialName
        var subscriptionId = subscriptionId
        var secretKey = secretKey
        var accessKey = accessKey
        var tenantId = tenantId
        var publicKeyFile = publicKeyFile
        // GIVEN
        credentialName = if (StringUtils.hasLength(credentialName)) credentialName else defaultName
        subscriptionId = if (StringUtils.hasLength(subscriptionId)) subscriptionId else defaultSubscriptionId
        secretKey = if (StringUtils.hasLength(secretKey)) secretKey else defaultSecretKey
        tenantId = if (StringUtils.hasLength(tenantId)) tenantId else defaultTenantId
        accessKey = if (StringUtils.hasLength(accessKey)) accessKey else defaultAccesKey

        publicKeyFile = if (StringUtils.hasLength(publicKeyFile)) publicKeyFile else defaultPublicKeyFile
        val publicKey = ResourceUtil.readStringFromResource(applicationContext, publicKeyFile).replace("\n".toRegex(), "")
        // WHEN
        // TODO publicInAccount
        val credentialRequest = CredentialRequest()
        credentialRequest.name = credentialName
        credentialRequest.publicKey = publicKey
        credentialRequest.description = "Azure Rm credential for integartiontest"
        val map = HashMap<String, Any>()
        map.put("subscriptionId", subscriptionId)
        map.put("tenantId", tenantId)
        map.put("accessKey", accessKey)
        map.put("secretKey", secretKey)
        credentialRequest.parameters = map
        credentialRequest.cloudPlatform = "AZURE_RM"
        val id = cloudbreakClient.credentialEndpoint().postPrivate(credentialRequest).id!!.toString()
        // THEN
        Assert.assertNotNull(id)
        itContext.putContextParam(CloudbreakITContextConstants.CREDENTIAL_ID, id, true)
    }
}
