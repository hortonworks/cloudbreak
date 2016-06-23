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

class GcpCredentialCreationTest : AbstractCloudbreakIntegrationTest() {
    @Value("${integrationtest.gcpcredential.name}")
    private val defaultName: String? = null
    @Value("${integrationtest.gcpcredential.projectId}")
    private val defaultProjectId: String? = null
    @Value("${integrationtest.gcpcredential.serviceAccountId}")
    private val defaultServiceAccountId: String? = null
    @Value("${integrationtest.gcpcredential.p12File}")
    private val defaultP12File: String? = null
    @Value("${integrationtest.gcpcredential.publicKeyFile}")
    private val defaultPublicKeyFile: String? = null

    @Test
    @Parameters("credentialName", "projectId", "serviceAccountId", "serviceAccountPrivateKeyP12File", "publicKeyFile")
    @Throws(Exception::class)
    fun testGCPCredentialCreation(@Optional("") credentialName: String, @Optional("") projectId: String, @Optional("") serviceAccountId: String,
                                  @Optional("") serviceAccountPrivateKeyP12File: String, @Optional("") publicKeyFile: String) {
        var credentialName = credentialName
        var projectId = projectId
        var serviceAccountId = serviceAccountId
        var serviceAccountPrivateKeyP12File = serviceAccountPrivateKeyP12File
        var publicKeyFile = publicKeyFile
        // GIVEN
        credentialName = if (StringUtils.hasLength(credentialName)) credentialName else defaultName
        projectId = if (StringUtils.hasLength(projectId)) projectId else defaultProjectId
        serviceAccountId = if (StringUtils.hasLength(serviceAccountId)) serviceAccountId else defaultServiceAccountId
        serviceAccountPrivateKeyP12File = if (StringUtils.hasLength(serviceAccountPrivateKeyP12File)) serviceAccountPrivateKeyP12File else defaultP12File
        publicKeyFile = if (StringUtils.hasLength(publicKeyFile)) publicKeyFile else defaultPublicKeyFile
        val serviceAccountPrivateKey = ResourceUtil.readBase64EncodedContentFromResource(applicationContext, serviceAccountPrivateKeyP12File)
        val publicKey = ResourceUtil.readStringFromResource(applicationContext, publicKeyFile).replace("\n".toRegex(), "")
        val credentialRequest = CredentialRequest()
        credentialRequest.cloudPlatform = "GCP"
        credentialRequest.description = "GCP credential for integartiontest"
        credentialRequest.name = credentialName
        credentialRequest.publicKey = publicKey
        val map = HashMap<String, Any>()
        map.put("projectId", projectId)
        map.put("serviceAccountId", serviceAccountId)
        map.put("serviceAccountPrivateKey", serviceAccountPrivateKey)
        credentialRequest.parameters = map
        // WHEN
        // TODO publicInAccount
        val id = cloudbreakClient.credentialEndpoint().postPrivate(credentialRequest).id!!.toString()
        // THEN
        Assert.assertNotNull(id)
        itContext.putContextParam(CloudbreakITContextConstants.CREDENTIAL_ID, id, true)
    }
}
