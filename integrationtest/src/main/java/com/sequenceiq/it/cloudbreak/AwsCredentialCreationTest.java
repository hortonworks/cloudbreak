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
public class AwsCredentialCreationTest extends AbstractCloudbreakIntegrationTest {
    private static final String URL_PATH = "/user/credentials";
    private static final String CREDENTIAL_NAME = "it-aws-cred";

    @Autowired
    private Template awsCredentialCreationTemplate;

    @Test
    @Parameters({ "credentialName", "roleArn", "publicKeyFile" })
    public void testAwsCredentialCreation(@Optional(CREDENTIAL_NAME) String credentialName, String roleArn,
            String publicKeyFile) throws IOException {
        // GIVEN
        IntegrationTestContext itContext = getItContext();
        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put("credentialName", credentialName);
        templateModel.put("roleArn", roleArn);
        templateModel.put("publicKey", ResourceUtil.readStringFromResource(applicationContext, publicKeyFile).replaceAll("\n", ""));
        // WHEN
        Response resourceCreationResponse = RestUtil.createEntityRequest(itContext.getContextParam(CloudbreakITContextConstants.CLOUDBREAK_SERVER),
                itContext.getContextParam(IntegrationTestContext.AUTH_TOKEN), FreeMarkerUtil.renderTemplate(awsCredentialCreationTemplate, templateModel))
        .log().all().post(URL_PATH);
        // THEN
        checkResponse(resourceCreationResponse, HttpStatus.CREATED, ContentType.JSON);
        itContext.putContextParam(CloudbreakITContextConstants.CREDENTIAL_ID, resourceCreationResponse.jsonPath().getString("id"), true);
    }
}
