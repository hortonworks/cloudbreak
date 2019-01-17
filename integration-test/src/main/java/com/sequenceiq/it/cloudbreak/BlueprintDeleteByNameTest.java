package com.sequenceiq.it.cloudbreak;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

public class BlueprintDeleteByNameTest extends AbstractCloudbreakIntegrationTest {
    @Test
    @Parameters("blueprintName")
    public void testDeleteBlueprintByName(String blueprintName) {
        // GIVEN
        Long workspaceId = getItContext().getContextParam(CloudbreakITContextConstants.WORKSPACE_ID, Long.class);
        // WHEN
        getCloudbreakClient().blueprintV4Endpoint().delete(workspaceId, blueprintName);
        // THEN no exception
    }
}
