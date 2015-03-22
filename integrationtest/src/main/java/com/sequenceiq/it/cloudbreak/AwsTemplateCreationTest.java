package com.sequenceiq.it.cloudbreak;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.config.IntegrationTestConfiguration;
import com.sequenceiq.it.util.FreeMarkerUtil;
import com.sequenceiq.it.util.RestUtil;

import freemarker.template.Template;

@ContextConfiguration(classes = IntegrationTestConfiguration.class)
public class AwsTemplateCreationTest extends AbstractCloudbreakIntegrationTest {
    @Autowired
    private TemplateAdditionHelper templateAdditionHelper;

    @Autowired
    private Template awsTemplateCreationTemplate;
    private List<TemplateAddition> additions;

    @BeforeMethod
    @Parameters({ "templateAdditions" })
    public void setup(@Optional("master,1;slave_1,3") String templateAdditions) {
        additions = templateAdditionHelper.parseTemplateAdditions(templateAdditions);
    }

    @Test
    @Parameters({ "awsTemplateName", "awsInstanceType", "awsVolumeType", "awsVolumeCount", "awsVolumeSize" })
    public void testAwsTemplateCreation(@Optional("it-aws-template") String awsTemplateName, @Optional("T2Medium") String awsInstanceType,
            @Optional("Standard") String awsVolumeType, @Optional("1") String awsVolumeCount, @Optional("10") String awsVolumeSize) {
        // GIVEN
        IntegrationTestContext itContext = getItContext();
        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put("awsTemplateName", awsTemplateName);
        templateModel.put("awsInstanceType", awsInstanceType);
        templateModel.put("awsVolumeType", awsVolumeType);
        templateModel.put("awsVolumeCount", awsVolumeCount);
        templateModel.put("awsVolumeSize", awsVolumeSize);
        // WHEN
        Response resourceCreationResponse = RestUtil.createEntityRequest(itContext.getContextParam(CloudbreakITContextConstants.CLOUDBREAK_SERVER),
                itContext.getContextParam(IntegrationTestContext.AUTH_TOKEN), FreeMarkerUtil.renderTemplate(awsTemplateCreationTemplate, templateModel))
                .post("/user/templates");
        // THEN
        checkResponse(resourceCreationResponse, HttpStatus.CREATED, ContentType.JSON);
        templateAdditionHelper.handleTemplateAdditions(itContext, resourceCreationResponse.jsonPath().getString("id"), additions);
    }
}
