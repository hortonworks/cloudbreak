package com.sequenceiq.it.cloudbreak;

import java.util.List;

import javax.inject.Inject;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.model.TemplateRequest;

public class OpenStackTemplateCreationTest extends AbstractCloudbreakIntegrationTest {
    @Inject
    private TemplateAdditionHelper additionHelper;

    private List<TemplateAddition> additions;

    @BeforeMethod
    @Parameters({ "templateAdditions" })
    public void setup(@Optional("master,1;slave_1,3") String templateAdditions) {
        additions = additionHelper.parseTemplateAdditions(templateAdditions);
    }

    @Test
    @Parameters({ "templateName", "instanceType", "volumeCount", "volumeSize" })
    public void testGcpTemplateCreation(@Optional("it-openstack-template") String templateName, @Optional("m1.large") String instanceType,
            @Optional("1") String volumeCount, @Optional("10") String volumeSize) throws Exception {
        // GIVEN
        // WHEN
        // TODO: publicInAccount
        TemplateRequest templateRequest = new TemplateRequest();
        templateRequest.setName(templateName);
        templateRequest.setDescription("OpenStack template for integration testing");
        templateRequest.setCloudPlatform("OPENSTACK");
        templateRequest.setInstanceType(instanceType);
        templateRequest.setVolumeCount(Integer.valueOf(volumeCount));
        templateRequest.setVolumeSize(Integer.valueOf(volumeSize));
        String id = getCloudbreakClient().templateEndpoint().postPrivate(templateRequest).getId().toString();
        // THEN
        Assert.assertNotNull(id);
        additionHelper.handleTemplateAdditions(getItContext(), id, additions);
    }
}
