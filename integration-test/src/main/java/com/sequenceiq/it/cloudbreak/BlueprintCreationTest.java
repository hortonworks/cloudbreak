package com.sequenceiq.it.cloudbreak;

import org.testng.Assert;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.it.util.ResourceUtil;

public class BlueprintCreationTest extends AbstractCloudbreakIntegrationTest {
    @Test
    @Parameters({ "blueprintName", "blueprintFile" })
    public void testBlueprintCreation(@Optional("it-hdp-multi-blueprint") String blueprintName,
            @Optional("classpath:/blueprint/hdp-multinode-default.bp") String blueprintFile) throws Exception {
        // GIVEN
        String blueprintContent = ResourceUtil.readStringFromResource(applicationContext, blueprintFile);
        // WHEN
        // TODO publicInAccount
        String id = getClient().postBlueprint(blueprintName, "Blueprint for integration testing", blueprintContent, false);
        // THEN
        Assert.assertNotNull(id);
        getItContext().putContextParam(CloudbreakITContextConstants.BLUEPRINT_ID, id, true);
    }
}
