package com.sequenceiq.it.cloudbreak;

import java.util.List;

import javax.inject.Inject;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

public class AzureTemplateCreationTest extends AbstractCloudbreakIntegrationTest {
    @Inject
    private TemplateAdditionHelper templateAdditionHelper;

    private List<TemplateAddition> additions;

    @BeforeMethod
    @Parameters({ "templateAdditions" })
    public void setup(@Optional("master,1;slave_1,3") String templateAdditions) {
        additions = templateAdditionHelper.parseTemplateAdditions(templateAdditions);
    }

    @Test
    @Parameters({ "azureTemplateName", "azureVmType", "azureVolumeCount", "azureVolumeSize" })
    public void testAzureTemplateCreation(@Optional("it-azure-template") String azureTemplateName, @Optional("MEDIUM") String azureVmType,
            @Optional("1") String azureVolumeCount, @Optional("10") String azureVolumeSize) throws Exception {
        // GIVEN
        // WHEN
        // TODO publicInAccount
        String id = getClient().postAzureTemplate(azureTemplateName, "azure integration test template", azureVmType, azureVolumeCount, azureVolumeSize, false);
        // THEN
        Assert.assertNotNull(id);
        templateAdditionHelper.handleTemplateAdditions(getItContext(), id, additions);
    }
}
