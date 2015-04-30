package com.sequenceiq.it.cloudbreak;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

public class TemplateDeleteByNameTest extends AbstractCloudbreakIntegrationTest {
    @Test
    @Parameters({ "templateName" })
    public void testDeleteTemplateByName(String templateName) throws Exception {
        // GIVEN
        // WHEN
        getClient().deleteTemplateByName(templateName);
        // THEN no exception
    }
}
