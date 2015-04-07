package com.sequenceiq.it.cloudbreak;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

public class GCCTemplateCreationTest extends AbstractCloudbreakIntegrationTest {
    @Autowired
    private TemplateAdditionHelper additionHelper;

    private List<TemplateAddition> additions;

    @BeforeMethod
    @Parameters({ "templateAdditions" })
    public void setup(@Optional("master,1;slave_1,3") String templateAdditions) {
        additions = additionHelper.parseTemplateAdditions(templateAdditions);
    }

    @Test
    @Parameters({ "gccName", "gccInstanceType", "volumeType", "volumeCount", "volumeSize" })
    public void testGCCTemplateCreation(@Optional("it-gcc-template") String gccName, @Optional("N1_STANDARD_2") String gccInstanceType,
            @Optional("HDD") String volumeType, @Optional("1") String volumeCount, @Optional("30") String volumeSize) throws Exception {
        // GIVEN
        // WHEN
        // TODO: VolumeType is missing from rest client, publicInAccount
        String id = getClient().postGccTemplate(gccName, "GCC template for integration testing", gccInstanceType, volumeCount, volumeSize, false);
        // THEN
        Assert.assertNotNull(id);
        additionHelper.handleTemplateAdditions(getItContext(), id, additions);
    }
}
