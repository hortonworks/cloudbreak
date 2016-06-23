package com.sequenceiq.it.cloudbreak

import org.testng.Assert
import org.testng.annotations.Optional
import org.testng.annotations.Parameters
import org.testng.annotations.Test

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.sequenceiq.cloudbreak.api.model.BlueprintRequest
import com.sequenceiq.it.util.ResourceUtil

class BlueprintCreationTest : AbstractCloudbreakIntegrationTest() {
    private val mapper = ObjectMapper()

    @Test
    @Parameters("blueprintName", "blueprintFile")
    @Throws(Exception::class)
    fun testBlueprintCreation(@Optional("it-hdp-multi-blueprint") blueprintName: String,
                              @Optional("classpath:/blueprint/hdp-multinode-default.bp") blueprintFile: String) {
        // GIVEN
        val blueprintContent = ResourceUtil.readStringFromResource(applicationContext, blueprintFile)
        // WHEN
        // TODO publicInAccount
        val blueprintRequest = BlueprintRequest()
        blueprintRequest.name = blueprintName
        blueprintRequest.description = "Blueprint for integration testing"
        blueprintRequest.setAmbariBlueprint(mapper.readValue<JsonNode>(blueprintContent, JsonNode::class.java))
        val id = cloudbreakClient.blueprintEndpoint().postPrivate(blueprintRequest).id!!.toString()
        // THEN
        Assert.assertNotNull(id)
        itContext.putContextParam(CloudbreakITContextConstants.BLUEPRINT_ID, id, true)
    }
}
