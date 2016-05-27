package com.sequenceiq.it.cloudbreak.mock;

import java.util.List;

import javax.inject.Inject;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.model.TemplateRequest;
import com.sequenceiq.it.cloudbreak.AbstractCloudbreakIntegrationTest;
import com.sequenceiq.it.cloudbreak.TemplateAddition;
import com.sequenceiq.it.cloudbreak.TemplateAdditionHelper;

public class MockTemplateCreationTest extends AbstractCloudbreakIntegrationTest {
    @Inject
    private TemplateAdditionHelper additionHelper;

    private List<TemplateAddition> additions;

    @BeforeMethod
    @Parameters({ "templateAdditions" })
    public void setup(@Optional("master,1;slave_1,3") String templateAdditions) {
        additions = additionHelper.parseTemplateAdditions(templateAdditions);
    }

    @Test
    @Parameters({ "mockName", "mockInstanceType", "volumeType", "volumeCount", "volumeSize" })
    public void testGcpTemplateCreation(@Optional("it-mock-template") String templateName, @Optional("small") String mockInstanceType,
            @Optional("magnetic") String volumeType, @Optional("1") String volumeCount, @Optional("30") String volumeSize) throws Exception {
        // GIVEN
        // WHEN
        // TODO: publicInAccount
        TemplateRequest templateRequest = new TemplateRequest();
        templateRequest.setName(templateName);
        templateRequest.setDescription("MOCK template for integration testing");
        templateRequest.setInstanceType(mockInstanceType);
        templateRequest.setVolumeCount(Integer.valueOf(volumeCount));
        templateRequest.setVolumeSize(Integer.valueOf(volumeSize));
        templateRequest.setVolumeType(volumeType);
        templateRequest.setCloudPlatform("MOCK");
        String id = getCloudbreakClient().templateEndpoint().postPrivate(templateRequest).getId().toString();
        // THEN
        Assert.assertNotNull(id);
        additionHelper.handleTemplateAdditions(getItContext(), id, additions);
    }
}
