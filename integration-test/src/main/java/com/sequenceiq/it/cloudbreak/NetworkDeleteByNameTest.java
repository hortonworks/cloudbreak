package com.sequenceiq.it.cloudbreak;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

public class NetworkDeleteByNameTest extends AbstractCloudbreakIntegrationTest {
    @Test
    @Parameters({ "networkName" })
    public void testDeleteTemplateByName(String networkName) throws Exception {
        // GIVEN
        // WHEN
        getClient().deleteTemplateByName(networkName);
        // THEN no exception
    }
}
