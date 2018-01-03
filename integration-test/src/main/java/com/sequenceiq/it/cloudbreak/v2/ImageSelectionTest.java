package com.sequenceiq.it.cloudbreak.v2;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.model.imagecatalog.ImagesResponse;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.AbstractCloudbreakIntegrationTest;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;

public class ImageSelectionTest extends AbstractCloudbreakIntegrationTest {
    @Test
    public void testImageSelection() throws Exception {
        // GIVEN
        IntegrationTestContext itContext = getItContext();
        // WHEN
        ImagesResponse imagesResponse = getCloudbreakClient().imageCatalogEndpoint().getImagesByProvider(
                itContext.getContextParam(CloudbreakITContextConstants.CLOUDPROVIDER));
        // THEN
        Assert.assertFalse(imagesResponse.getBaseImages().isEmpty());
        itContext.putContextParam(CloudbreakV2Constants.IMAGEID, imagesResponse.getBaseImages().get(0).getUuid());
    }
}
