package com.sequenceiq.it.cloudbreak

import javax.inject.Inject

import org.testng.Assert
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Optional
import org.testng.annotations.Parameters
import org.testng.annotations.Test

import com.sequenceiq.cloudbreak.api.model.TemplateRequest

class AzureTemplateCreationTest : AbstractCloudbreakIntegrationTest() {
    @Inject
    private val templateAdditionHelper: TemplateAdditionHelper? = null

    private var additions: List<TemplateAddition>? = null

    @BeforeMethod
    @Parameters("templateAdditions")
    fun setup(@Optional("master,1;slave_1,3") templateAdditions: String) {
        additions = templateAdditionHelper!!.parseTemplateAdditions(templateAdditions)
    }

    @Test
    @Parameters("azureTemplateName", "azureVmType", "azureVolumeCount", "azureVolumeSize")
    @Throws(Exception::class)
    fun testAzureTemplateCreation(@Optional("it-azure-template") azureTemplateName: String, @Optional("MEDIUM") azureVmType: String,
                                  @Optional("1") azureVolumeCount: String, @Optional("10") azureVolumeSize: String) {
        // GIVEN
        // WHEN
        // TODO publicInAccount
        val templateRequest = TemplateRequest()
        templateRequest.name = azureTemplateName
        templateRequest.description = "AZURE_RM template for integration testing"
        templateRequest.cloudPlatform = "AZURE_RM"
        templateRequest.instanceType = azureVmType
        templateRequest.volumeType = "Standard_LRS"
        templateRequest.volumeCount = Integer.valueOf(azureVolumeCount)
        templateRequest.volumeSize = Integer.valueOf(azureVolumeSize)
        val id = cloudbreakClient.templateEndpoint().postPrivate(templateRequest).id!!.toString()
        // THEN
        Assert.assertNotNull(id)
        templateAdditionHelper!!.handleTemplateAdditions(itContext, id, additions)
    }
}
