package com.sequenceiq.it.cloudbreak

import java.util.HashMap

import javax.inject.Inject

import org.testng.Assert
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Optional
import org.testng.annotations.Parameters
import org.testng.annotations.Test

import com.sequenceiq.cloudbreak.api.model.TemplateRequest

class AwsTemplateCreationTest : AbstractCloudbreakIntegrationTest() {
    @Inject
    private val templateAdditionHelper: TemplateAdditionHelper? = null

    private var additions: List<TemplateAddition>? = null

    @BeforeMethod
    @Parameters("templateAdditions")
    fun setup(@Optional("master,1;slave_1,3") templateAdditions: String) {
        additions = templateAdditionHelper!!.parseTemplateAdditions(templateAdditions)
    }

    @Test
    @Parameters("awsTemplateName", "awsInstanceType", "awsVolumeType", "awsVolumeCount", "awsVolumeSize")
    @Throws(Exception::class)
    fun testAwsTemplateCreation(@Optional("it-aws-template") awsTemplateName: String, @Optional("m3.medium") awsInstanceType: String,
                                @Optional("standard") awsVolumeType: String, @Optional("1") awsVolumeCount: String, @Optional("10") awsVolumeSize: String) {
        // GIVEN
        // WHEN
        // TODO PublicInAccount, Encrypted
        val templateRequest = TemplateRequest()
        templateRequest.name = awsTemplateName
        templateRequest.description = "AWS template for integration testing"
        templateRequest.cloudPlatform = "AWS"
        templateRequest.instanceType = awsInstanceType
        templateRequest.volumeCount = Integer.valueOf(awsVolumeCount)
        templateRequest.volumeSize = Integer.valueOf(awsVolumeSize)
        templateRequest.volumeType = awsVolumeType
        val map = HashMap<String, Any>()
        map.put("encrypted", false)
        templateRequest.parameters = map
        templateRequest.cloudPlatform = "AWS"
        val id = cloudbreakClient.templateEndpoint().postPrivate(templateRequest).id!!.toString()
        // THEN
        Assert.assertNotNull(id)
        templateAdditionHelper!!.handleTemplateAdditions(itContext, id, additions)
    }
}
