package com.sequenceiq.it.cloudbreak.action.sdx.saas;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxSaasTestDto;
import com.sequenceiq.it.cloudbreak.microservice.SdxSaasItClient;

public class SdxSaasDeleteAction implements Action<SdxSaasTestDto, SdxSaasItClient> {
    @Override
    public SdxSaasTestDto action(TestContext testContext, SdxSaasTestDto testDto, SdxSaasItClient client) throws Exception {
        client.getDefaultClient().deleteInstance(testDto.getCrn());
        return testDto;
    }
}
