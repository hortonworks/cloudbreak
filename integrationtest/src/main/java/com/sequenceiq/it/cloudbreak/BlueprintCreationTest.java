package com.sequenceiq.it.cloudbreak;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.config.IntegrationTestConfiguration;
import com.sequenceiq.it.util.FreeMarkerUtil;
import com.sequenceiq.it.util.ResourceUtil;
import com.sequenceiq.it.util.RestUtil;

import freemarker.template.Template;

@ContextConfiguration(classes = IntegrationTestConfiguration.class)
public class BlueprintCreationTest extends AbstractCloudbreakIntegrationTest {
    @Autowired
    private Template blueprintCreationTemplate;

    @Test
    @Parameters({ "blueprintName", "blueprintFile" })
    public void testBlueprintCreation(@Optional("it-hdp-multi-blueprint") String blueprintName,
            @Optional("classpath:/blueprint/hdp-multinode-default.bp") String blueprintFile) throws IOException {
        // GIVEN
        IntegrationTestContext itContext = getItContext();
        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put("blueprintName", blueprintName);
        templateModel.put("blueprintContent", ResourceUtil.readStringFromResource(applicationContext, blueprintFile));
        // WHEN
        Response resourceCreationResponse = RestUtil.createEntityRequest(itContext.getContextParam(CloudbreakITContextConstants.CLOUDBREAK_SERVER),
                itContext.getContextParam(IntegrationTestContext.AUTH_TOKEN), FreeMarkerUtil.renderTemplate(blueprintCreationTemplate, templateModel))
                .post("/user/blueprints");
        // THEN
        checkResponse(resourceCreationResponse, HttpStatus.CREATED, ContentType.JSON);
        itContext.putContextParam(CloudbreakITContextConstants.BLUEPRINT_ID, resourceCreationResponse.jsonPath().getString("id"), true);
    }
}
