package com.sequenceiq.it.cloudbreak.action.freeipa;

import com.sequenceiq.freeipa.api.v1.util.model.UsedImagesListV1Response;
import com.sequenceiq.it.cloudbreak.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeipaUsedImagesTestDto;

public class FreeipaUsedImagesAction implements Action<FreeipaUsedImagesTestDto, FreeIpaClient> {

    @Override
    public FreeipaUsedImagesTestDto action(TestContext testContext, FreeipaUsedImagesTestDto testDto, FreeIpaClient client) throws Exception {
        final UsedImagesListV1Response usedImages = client.getInternalClient(testContext).utilV1Endpoint().usedImages(null);
        testDto.setResponse(usedImages);
        return testDto;
    }
}
