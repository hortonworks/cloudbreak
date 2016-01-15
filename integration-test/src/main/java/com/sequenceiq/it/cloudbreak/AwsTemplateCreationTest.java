package com.sequenceiq.it.cloudbreak;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.model.TemplateRequest;

public class AwsTemplateCreationTest extends AbstractCloudbreakIntegrationTest {
    @Inject
    private TemplateAdditionHelper templateAdditionHelper;

    private List<TemplateAddition> additions;

    @BeforeMethod
    @Parameters({ "templateAdditions" })
    public void setup(@Optional("master,1;slave_1,3") String templateAdditions) {
        additions = templateAdditionHelper.parseTemplateAdditions(templateAdditions);
    }

    @Test
    @Parameters({ "awsTemplateName", "awsInstanceType", "awsVolumeType", "awsVolumeCount", "awsVolumeSize" })
    public void testAwsTemplateCreation(@Optional("it-aws-template") String awsTemplateName, @Optional("m3.medium") String awsInstanceType,
            @Optional("standard") String awsVolumeType, @Optional("1") String awsVolumeCount, @Optional("10") String awsVolumeSize) throws Exception {
        // GIVEN
        // WHEN
        // TODO PublicInAccount, Encrypted
        TemplateRequest templateRequest = new TemplateRequest();
        templateRequest.setName(awsTemplateName);
        templateRequest.setDescription("AWS template for integration testing");
        templateRequest.setCloudPlatform("AWS");
        templateRequest.setInstanceType(awsInstanceType);
        templateRequest.setVolumeCount(Integer.valueOf(awsVolumeCount));
        templateRequest.setVolumeSize(Integer.valueOf(awsVolumeSize));
        templateRequest.setVolumeType(awsVolumeType);
        Map<String, Object> map = new HashMap<>();
        map.put("encrypted", false);
        templateRequest.setParameters(map);
        templateRequest.setCloudPlatform("AWS");
        String id = getTemplateEndpoint().postPrivate(templateRequest).getId().toString();
        // THEN
        Assert.assertNotNull(id);
        templateAdditionHelper.handleTemplateAdditions(getItContext(), id, additions);
    }
}
