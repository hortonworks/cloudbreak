package com.sequenceiq.it.cloudbreak

import java.util.Arrays

import org.testng.Assert
import org.testng.annotations.Optional
import org.testng.annotations.Parameters
import org.testng.annotations.Test

import com.sequenceiq.cloudbreak.api.model.SecurityGroupJson
import com.sequenceiq.cloudbreak.api.model.SecurityRuleJson

class SecurityGroupCreationTest : AbstractCloudbreakIntegrationTest() {

    @Test
    @Parameters("name", "ports")
    @Throws(Exception::class)
    fun testSecurityGroupCreation(@Optional("it-restricted-ambari") name: String, @Optional("22,443,9443,8080") ports: String) {
        // GIVEN
        // WHEN
        val securityGroupJson = SecurityGroupJson()
        securityGroupJson.description = "Security group created by IT"
        securityGroupJson.name = name
        val securityRuleJson = SecurityRuleJson()
        securityRuleJson.protocol = "tcp"
        securityRuleJson.subnet = "0.0.0.0/0"
        securityRuleJson.ports = ports
        securityGroupJson.securityRules = Arrays.asList(securityRuleJson)

        val id = cloudbreakClient.securityGroupEndpoint().postPrivate(securityGroupJson).id!!.toString()
        // THEN
        Assert.assertNotNull(id)
        itContext.putContextParam(CloudbreakITContextConstants.SECURITY_GROUP_ID, id, true)
    }

}
