package com.sequenceiq.it.cloudbreak;

import java.util.List;

import javax.inject.Inject;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.model.TemplateRequest;

import jersey.repackaged.com.google.common.collect.ImmutableMap;

public class GcpTemplateCreationTest extends AbstractCloudbreakIntegrationTest {
    @Inject
    private TemplateAdditionHelper additionHelper;

    private List<TemplateAddition> additions;

    @BeforeMethod
    @Parameters({ "templateAdditions" })
    public void setup(@Optional("master,1;slave_1,3") String templateAdditions) {
        additions = additionHelper.parseTemplateAdditions(templateAdditions);
    }

    @Test
    @Parameters({ "gcpName", "gcpInstanceType", "volumeType", "volumeCount", "volumeSize", "preemptible" })
    public void testGcpTemplateCreation(@Optional("it-gcp-template") String gcpName, @Optional("n1-standard-2") String gcpInstanceType,
            @Optional("pd-standard") String volumeType, @Optional("1") String volumeCount, @Optional("30") String volumeSize,
            @Optional("false") Boolean preemptible) throws Exception {
        // GIVEN
        // WHEN
        // TODO: publicInAccount
        TemplateRequest templateRequest = new TemplateRequest();
        templateRequest.setName(gcpName);
        templateRequest.setDescription("GCP template for integration testing");
        templateRequest.setCloudPlatform("GCP");
        templateRequest.setInstanceType(gcpInstanceType);
        templateRequest.setVolumeCount(Integer.valueOf(volumeCount));
        templateRequest.setVolumeSize(Integer.valueOf(volumeSize));
        templateRequest.setVolumeType(volumeType);
        templateRequest.setCloudPlatform("GCP");
        if (preemptible != null) {
            templateRequest.setParameters(ImmutableMap.of("preemptible", preemptible));
        }
        String id = getCloudbreakClient().templateEndpoint().postPrivate(templateRequest).getId().toString();
        // THEN
        Assert.assertNotNull(id);
        additionHelper.handleTemplateAdditions(getItContext(), id, additions);
    }

}
