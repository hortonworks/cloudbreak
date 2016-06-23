package com.sequenceiq.it.cloudbreak

import org.testng.annotations.Parameters
import org.testng.annotations.Test

class BlueprintDeleteByNameTest : AbstractCloudbreakIntegrationTest() {
    @Test
    @Parameters("blueprintName")
    @Throws(Exception::class)
    fun testDeleteBlueprintByName(blueprintName: String) {
        // GIVEN
        // WHEN
        cloudbreakClient.blueprintEndpoint().deletePublic(blueprintName)
        // THEN no exception
    }
}
