package com.sequenceiq.it.cloudbreak;

import org.apache.commons.codec.binary.Base64;
import org.testng.Assert;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.api.model.BlueprintRequest;
import com.sequenceiq.it.util.ResourceUtil;

public class BlueprintCreationTest extends AbstractCloudbreakIntegrationTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @Parameters({ "blueprintName", "blueprintFile" })
    public void testBlueprintCreation(@Optional("it-hdp-multi-blueprint") String blueprintName,
            @Optional("classpath:/blueprint/hdp-multinode-default.bp") String blueprintFile) throws Exception {
        // GIVEN
        String blueprintContent = ResourceUtil.readStringFromResource(applicationContext, blueprintFile);
        // WHEN
        BlueprintRequest blueprintRequest = new BlueprintRequest();
        blueprintRequest.setName(blueprintName);
        blueprintRequest.setDescription("Blueprint for integration testing");
        blueprintRequest.setAmbariBlueprint(Base64.encodeBase64String(blueprintContent.getBytes()));
        String id = getCloudbreakClient().blueprintEndpoint().postPrivate(blueprintRequest).getId().toString();
        // THEN
        Assert.assertNotNull(id);
        getItContext().putContextParam(CloudbreakITContextConstants.BLUEPRINT_ID, id, true);
    }
}
