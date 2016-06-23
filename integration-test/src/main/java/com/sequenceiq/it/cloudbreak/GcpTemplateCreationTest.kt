package com.sequenceiq.it.cloudbreak

import javax.inject.Inject

import org.testng.Assert
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Optional
import org.testng.annotations.Parameters
import org.testng.annotations.Test

import com.sequenceiq.cloudbreak.api.model.TemplateRequest

class GcpTemplateCreationTest : AbstractCloudbreakIntegrationTest() {
    @Inject
    private val additionHelper: TemplateAdditionHelper? = null

    private var additions: List<TemplateAddition>? = null

    @BeforeMethod
    @Parameters("templateAdditions")
    fun setup(@Optional("master,1;slave_1,3") templateAdditions: String) {
        additions = additionHelper!!.parseTemplateAdditions(templateAdditions)
    }

    @Test
    @Parameters("gcpName", "gcpInstanceType", "volumeType", "volumeCount", "volumeSize")
    @Throws(Exception::class)
    fun testGcpTemplateCreation(@Optional("it-gcp-template") gcpName: String, @Optional("n1-standard-2") gcpInstanceType: String,
                                @Optional("pd-standard") volumeType: String, @Optional("1") volumeCount: String, @Optional("30") volumeSize: String) {
        // GIVEN
        // WHEN
        // TODO: publicInAccount
        val templateRequest = TemplateRequest()
        templateRequest.name = gcpName
        templateRequest.description = "GCP template for integration testing"
        templateRequest.cloudPlatform = "GCP"
        templateRequest.instanceType = gcpInstanceType
        templateRequest.volumeCount = Integer.valueOf(volumeCount)
        templateRequest.volumeSize = Integer.valueOf(volumeSize)
        templateRequest.volumeType = volumeType
        templateRequest.cloudPlatform = "GCP"
        val id = cloudbreakClient.templateEndpoint().postPrivate(templateRequest).id!!.toString()
        // THEN
        Assert.assertNotNull(id)
        additionHelper!!.handleTemplateAdditions(itContext, id, additions)
    }
}
