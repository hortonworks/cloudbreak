package com.sequenceiq.it.cloudbreak;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

public class BlueprintDeleteByNameTest extends AbstractCloudbreakIntegrationTest {
    @Test
    @Parameters({ "blueprintName" })
    public void testDeleteBlueprintByName(String blueprintName) throws Exception {
        // GIVEN
        // WHEN
        getClient().deleteBlueprintByName(blueprintName);
        // THEN no exception
    }
}
