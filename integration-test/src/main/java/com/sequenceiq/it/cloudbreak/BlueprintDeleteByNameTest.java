package com.sequenceiq.it.cloudbreak;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

public class BlueprintDeleteByNameTest extends AbstractCloudbreakIntegrationTest {
    @Test
    @Parameters("blueprintName")
    public void testDeleteBlueprintByName(String blueprintName) {
        // GIVEN
        // WHEN
        getCloudbreakClient().blueprintEndpoint().deletePublic(blueprintName);
        // THEN no exception
    }
}
