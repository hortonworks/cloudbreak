package com.sequenceiq.it.cloudbreak

import org.testng.annotations.Parameters
import org.testng.annotations.Test

class TemplateDeleteByNameTest : AbstractCloudbreakIntegrationTest() {
    @Test
    @Parameters("templateName")
    @Throws(Exception::class)
    fun testDeleteTemplateByName(templateName: String) {
        // GIVEN
        // WHEN
        cloudbreakClient.templateEndpoint().deletePublic(templateName)
        // THEN no exception
    }
}
