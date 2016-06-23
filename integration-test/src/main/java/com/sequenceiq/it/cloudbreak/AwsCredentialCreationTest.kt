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

class AwsCredentialCreationTest : AbstractCloudbreakIntegrationTest() {
    @Value("${integrationtest.awscredential.name}")
    private val defaultName: String? = null
    @Value("${integrationtest.awscredential.roleArn:}")
    private val defaultRoleArn: String? = null
    @Value("${integrationtest.awscredential.accessKey:}")
    private val defaultAccessKey: String? = null
    @Value("${integrationtest.awscredential.secretKey:}")
    private val defaultSecretKey: String? = null
    @Value("${integrationtest.awscredential.publicKeyFile}")
    private val defaultPublicKeyFile: String? = null

    @Test
    @Parameters("credentialName", "roleArn", "accessKey", "secretKey", "publicKeyFile")
    @Throws(Exception::class)
    fun testAwsCredentialCreation(@Optional("") credentialName: String, @Optional("") roleArn: String?,
                                  @Optional("") accessKey: String, @Optional("") secretKey: String, @Optional("") publicKeyFile: String) {
        var credentialName = credentialName
        var roleArn = roleArn
        var accessKey = accessKey
        var secretKey = secretKey
        var publicKeyFile = publicKeyFile
        // GIVEN
        credentialName = if (StringUtils.hasLength(credentialName)) credentialName else defaultName
        roleArn = if (StringUtils.hasLength(roleArn)) roleArn else defaultRoleArn
        accessKey = if (StringUtils.hasLength(accessKey)) accessKey else defaultAccessKey
        secretKey = if (StringUtils.hasLength(secretKey)) secretKey else defaultSecretKey
        publicKeyFile = if (StringUtils.hasLength(publicKeyFile)) publicKeyFile else defaultPublicKeyFile
        val publicKey = ResourceUtil.readStringFromResource(applicationContext, publicKeyFile).replace("\n".toRegex(), "")
        // TODO publicInAccount
        val credentialRequest = CredentialRequest()
        credentialRequest.name = credentialName
        credentialRequest.publicKey = publicKey
        credentialRequest.description = "Aws credential for integrationtest"
        val map = HashMap<String, Any>()
        if (roleArn != null && "" != roleArn) {
            map.put("selector", "role-based")
            map.put("roleArn", roleArn)
        } else {
            map.put("selector", "key-based")
            map.put("accessKey", accessKey)
            map.put("secretKey", secretKey)
        }
        credentialRequest.parameters = map
        credentialRequest.cloudPlatform = "AWS"
        // WHEN
        // TODO publicInAccount
        val id = cloudbreakClient.credentialEndpoint().postPrivate(credentialRequest).id!!.toString()
        // THEN
        Assert.assertNotNull(id)
        itContext.putContextParam(CloudbreakITContextConstants.CREDENTIAL_ID, id, true)
    }
}
