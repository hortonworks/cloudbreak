package com.sequenceiq.it.cloudbreak.action.v4.util;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.UsedImagesListV4Response;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.util.UsedImagesTestDto;

public class UsedImagesAction implements Action<UsedImagesTestDto, CloudbreakClient> {

    @Override
    public UsedImagesTestDto action(TestContext testContext, UsedImagesTestDto testDto, CloudbreakClient client) throws Exception {
        final UsedImagesListV4Response usedImages = client.getInternalClient(testContext).utilV4Endpoint().usedImages();
        testDto.setResponse(usedImages);
        return testDto;
    }
}
