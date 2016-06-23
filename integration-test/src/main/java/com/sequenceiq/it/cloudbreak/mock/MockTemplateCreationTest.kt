package com.sequenceiq.it.cloudbreak.mock

import javax.inject.Inject

import org.testng.Assert
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Optional
import org.testng.annotations.Parameters
import org.testng.annotations.Test

import com.sequenceiq.cloudbreak.api.model.TemplateRequest
import com.sequenceiq.it.cloudbreak.AbstractCloudbreakIntegrationTest
import com.sequenceiq.it.cloudbreak.TemplateAddition
import com.sequenceiq.it.cloudbreak.TemplateAdditionHelper

class MockTemplateCreationTest : AbstractCloudbreakIntegrationTest() {
    @Inject
    private val additionHelper: TemplateAdditionHelper? = null

    private var additions: List<TemplateAddition>? = null

    @BeforeMethod
    @Parameters("templateAdditions")
    fun setup(@Optional("master,1;slave_1,3") templateAdditions: String) {
        additions = additionHelper!!.parseTemplateAdditions(templateAdditions)
    }

    @Test
    @Parameters("mockName", "mockInstanceType", "volumeType", "volumeCount", "volumeSize")
    @Throws(Exception::class)
    fun testGcpTemplateCreation(@Optional("it-mock-template") templateName: String, @Optional("small") mockInstanceType: String,
                                @Optional("magnetic") volumeType: String, @Optional("1") volumeCount: String, @Optional("30") volumeSize: String) {
        // GIVEN
        // WHEN
        // TODO: publicInAccount
        val templateRequest = TemplateRequest()
        templateRequest.name = templateName
        templateRequest.description = "MOCK template for integration testing"
        templateRequest.instanceType = mockInstanceType
        templateRequest.volumeCount = Integer.valueOf(volumeCount)
        templateRequest.volumeSize = Integer.valueOf(volumeSize)
        templateRequest.volumeType = volumeType
        templateRequest.cloudPlatform = "MOCK"
        val id = cloudbreakClient.templateEndpoint().postPrivate(templateRequest).id!!.toString()
        // THEN
        Assert.assertNotNull(id)
        additionHelper!!.handleTemplateAdditions(itContext, id, additions)
    }
}
