package com.sequenceiq.it.cloudbreak;

import org.testng.Assert;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprints.requests.BlueprintV4Request;
import com.sequenceiq.it.util.ResourceUtil;

public class BlueprintCreationTest extends AbstractCloudbreakIntegrationTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @Parameters({ "blueprintName", "blueprintFile" })
    public void testBlueprintCreation(@Optional("it-hdp-multi-blueprint") String blueprintName,
            @Optional("classpath:/blueprint/hdp-multinode-default.bp") String blueprintFile) throws Exception {
        // GIVEN
        String blueprintContent = ResourceUtil.readStringFromResource(applicationContext, blueprintFile);
        Long workspaceId = getItContext().getContextParam(CloudbreakITContextConstants.WORKSPACE_ID, Long.class);
        // WHEN
        BlueprintV4Request blueprintRequest = new BlueprintV4Request();
        blueprintRequest.setName(blueprintName);
        blueprintRequest.setDescription("Blueprint for integration testing");
        blueprintRequest.setAmbariBlueprint(blueprintContent);
        String id = getCloudbreakClient().blueprintV4Endpoint().post(workspaceId, blueprintRequest).getId().toString();
        // THEN
        Assert.assertNotNull(id);
        getItContext().putContextParam(CloudbreakITContextConstants.BLUEPRINT_ID, id, true);
    }
}
