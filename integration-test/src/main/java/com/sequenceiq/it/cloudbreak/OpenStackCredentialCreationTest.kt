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

class OpenStackCredentialCreationTest : AbstractCloudbreakIntegrationTest() {
    @Value("${integrationtest.openstackcredential.name}")
    private val defaultName: String? = null
    @Value("${integrationtest.openstackcredential.tenantName}")
    private val defaultTenantName: String? = null
    @Value("${integrationtest.openstackcredential.userName}")
    private val defaultUserName: String? = null
    @Value("${integrationtest.openstackcredential.password}")
    private val defaultPassword: String? = null
    @Value("${integrationtest.openstackcredential.endpoint}")
    private val defaultEndpoint: String? = null
    @Value("${integrationtest.openstackcredential.publicKeyFile}")
    private val defaultPublicKeyFile: String? = null

    @Test
    @Parameters("credentialName", "tenantName", "userName", "password", "endpoint", "publicKeyFile")
    @Throws(Exception::class)
    fun testOpenStackCredentialCreation(@Optional("") credentialName: String, @Optional("") tenantName: String, @Optional("") userName: String,
                                        @Optional("") password: String, @Optional("") endpoint: String, @Optional("") publicKeyFile: String) {
        var credentialName = credentialName
        var tenantName = tenantName
        var userName = userName
        var password = password
        var endpoint = endpoint
        var publicKeyFile = publicKeyFile
        // GIVEN
        credentialName = if (StringUtils.hasLength(credentialName)) credentialName else defaultName
        tenantName = if (StringUtils.hasLength(tenantName)) tenantName else defaultTenantName
        userName = if (StringUtils.hasLength(userName)) userName else defaultUserName
        password = if (StringUtils.hasLength(password)) password else defaultPassword
        endpoint = if (StringUtils.hasLength(endpoint)) endpoint else defaultEndpoint
        publicKeyFile = if (StringUtils.hasLength(publicKeyFile)) publicKeyFile else defaultPublicKeyFile
        val publicKey = ResourceUtil.readStringFromResource(applicationContext, publicKeyFile).replace("\n".toRegex(), "")

        val credentialRequest = CredentialRequest()
        credentialRequest.name = credentialName
        credentialRequest.publicKey = publicKey
        credentialRequest.description = "Aws Rm credential for integartiontest"
        val map = HashMap<String, Any>()
        map.put("tenantName", tenantName)
        map.put("userName", userName)
        map.put("password", password)
        map.put("endpoint", endpoint)
        map.put("keystoneVersion", "cb-keystone-v2")
        map.put("selector", "cb-keystone-v2")

        credentialRequest.parameters = map
        credentialRequest.cloudPlatform = "OPENSTACK"
        // WHEN
        // TODO publicInAccount
        val id = cloudbreakClient.credentialEndpoint().postPrivate(credentialRequest).id!!.toString()
        // THEN
        Assert.assertNotNull(id)
        itContext.putContextParam(CloudbreakITContextConstants.CREDENTIAL_ID, id, true)
    }
}
