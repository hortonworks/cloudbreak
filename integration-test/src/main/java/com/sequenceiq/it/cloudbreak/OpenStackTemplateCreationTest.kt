package com.sequenceiq.it.cloudbreak

import javax.inject.Inject

import org.testng.Assert
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Optional
import org.testng.annotations.Parameters
import org.testng.annotations.Test

import com.sequenceiq.cloudbreak.api.model.TemplateRequest

class OpenStackTemplateCreationTest : AbstractCloudbreakIntegrationTest() {
    @Inject
    private val additionHelper: TemplateAdditionHelper? = null

    private var additions: List<TemplateAddition>? = null

    @BeforeMethod
    @Parameters("templateAdditions")
    fun setup(@Optional("master,1;slave_1,3") templateAdditions: String) {
        additions = additionHelper!!.parseTemplateAdditions(templateAdditions)
    }

    @Test
    @Parameters("templateName", "instanceType", "volumeCount", "volumeSize")
    @Throws(Exception::class)
    fun testGcpTemplateCreation(@Optional("it-openstack-template") templateName: String, @Optional("m1.large") instanceType: String,
                                @Optional("1") volumeCount: String, @Optional("10") volumeSize: String) {
        // GIVEN
        // WHEN
        // TODO: publicInAccount
        val templateRequest = TemplateRequest()
        templateRequest.name = templateName
        templateRequest.description = "OpenStack template for integration testing"
        templateRequest.cloudPlatform = "OPENSTACK"
        templateRequest.volumeType = "HDD"
        templateRequest.instanceType = instanceType
        templateRequest.volumeCount = Integer.valueOf(volumeCount)
        templateRequest.volumeSize = Integer.valueOf(volumeSize)
        val id = cloudbreakClient.templateEndpoint().postPrivate(templateRequest).id!!.toString()
        // THEN
        Assert.assertNotNull(id)
        additionHelper!!.handleTemplateAdditions(itContext, id, additions)
    }
}
