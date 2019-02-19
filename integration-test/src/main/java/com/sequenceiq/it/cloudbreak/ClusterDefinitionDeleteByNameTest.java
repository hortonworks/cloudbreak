package com.sequenceiq.it.cloudbreak;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

public class ClusterDefinitionDeleteByNameTest extends AbstractCloudbreakIntegrationTest {
    @Test
    @Parameters("clusterDefinitionName")
    public void testDeleteClusterDefinitionByName(String clusterDefinitionName) {
        // GIVEN
        Long workspaceId = getItContext().getContextParam(CloudbreakITContextConstants.WORKSPACE_ID, Long.class);
        // WHEN
        getCloudbreakClient().clusterDefinitionV4Endpoint().delete(workspaceId, clusterDefinitionName);
        // THEN no exception
    }
}
