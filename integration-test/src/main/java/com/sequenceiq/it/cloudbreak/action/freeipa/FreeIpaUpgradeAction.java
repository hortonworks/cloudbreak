package com.sequenceiq.it.cloudbreak.action.freeipa;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.util.Strings;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.upgrade.model.FreeIpaUpgradeRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.upgrade.model.FreeIpaUpgradeResponse;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;

public class FreeIpaUpgradeAction extends AbstractFreeIpaAction<FreeIpaTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaUpgradeAction.class);

    private final String imageCatalogUrl;

    private final String imageId;

    public FreeIpaUpgradeAction() {
        imageCatalogUrl = null;
        imageId = null;
    }

    public FreeIpaUpgradeAction(String imageCatalogUrl, String imageId) {
        this.imageCatalogUrl = imageCatalogUrl;
        this.imageId = imageId;
    }

    public FreeIpaTestDto freeIpaAction(TestContext testContext, FreeIpaTestDto testDto, FreeIpaClient client) throws Exception {
        FreeIpaUpgradeRequest request = new FreeIpaUpgradeRequest();
        request.setEnvironmentCrn(testDto.getRequest().getEnvironmentCrn());
        request.setAllowMajorOsUpgrade(Boolean.TRUE);
        if (Strings.isNotNullAndNotEmpty(imageCatalogUrl) && Strings.isNotNullAndNotEmpty(imageId)) {
            ImageSettingsRequest imageSettingsRequest = new ImageSettingsRequest();
            imageSettingsRequest.setCatalog(imageCatalogUrl);
            imageSettingsRequest.setId(imageId);
            request.setImage(imageSettingsRequest);
        }
        Log.whenJson(LOGGER, format(" FreeIPA upgrade request:%n"), request);
        FreeIpaUpgradeResponse response = client.getDefaultClient()
                .getFreeIpaUpgradeV1Endpoint()
                .upgradeFreeIpa(request);
        testDto.setFlow("FreeIPA upgrade", response.getFlowIdentifier());
        testDto.setOperationId(response.getOperationId());
        Log.whenJson(LOGGER, format(" FreeIPA upgrade started: %n"), response);
        return testDto;
    }
}
