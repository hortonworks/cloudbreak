package com.sequenceiq.it.cloudbreak;

import java.util.List;

import javax.inject.Inject;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

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
    public void testAwsTemplateCreation(@Optional("it-aws-template") String awsTemplateName, @Optional("T2Medium") String awsInstanceType,
            @Optional("Standard") String awsVolumeType, @Optional("1") String awsVolumeCount, @Optional("10") String awsVolumeSize) throws Exception {
        // GIVEN
        // WHEN
        // TODO PublicInAccount, Encrypted
        String id = getClient().postEc2Template(awsTemplateName, "Integration Test Template", "0.0.0.0/0", awsInstanceType, awsVolumeCount, awsVolumeSize,
                awsVolumeType, false, false);
        // THEN
        Assert.assertNotNull(id);
        templateAdditionHelper.handleTemplateAdditions(getItContext(), id, additions);
    }
}
