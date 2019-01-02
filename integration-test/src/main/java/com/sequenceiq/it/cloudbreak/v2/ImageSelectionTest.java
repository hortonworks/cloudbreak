package com.sequenceiq.it.cloudbreak.v2;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImagesV4Response;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.AbstractCloudbreakIntegrationTest;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;

public class ImageSelectionTest extends AbstractCloudbreakIntegrationTest {
    @Test
    public void testImageSelection() throws Exception {
        // GIVEN
        IntegrationTestContext itContext = getItContext();

        // WHEN
        String provider = itContext.getContextParam(CloudbreakITContextConstants.CLOUDPROVIDER);
        Long workspaceId = itContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        ImagesV4Response imagesV4Response = getCloudbreakClient().imageCatalogV4Endpoint().getImages(workspaceId, null, provider);
        // THEN
        Assert.assertFalse(imagesV4Response.getBaseImages().isEmpty());
        itContext.putContextParam(CloudbreakV2Constants.IMAGEID, imagesV4Response.getBaseImages().get(0).getUuid());
    }
}
