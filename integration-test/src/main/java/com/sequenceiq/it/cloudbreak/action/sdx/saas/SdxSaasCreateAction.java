package com.sequenceiq.it.cloudbreak.action.sdx.saas;

import com.sequenceiq.it.cloudbreak.SdxSaasItClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxSaasTestDto;

public class SdxSaasCreateAction implements Action<SdxSaasTestDto, SdxSaasItClient> {
    @Override
    public SdxSaasTestDto action(TestContext testContext, SdxSaasTestDto testDto, SdxSaasItClient client) throws Exception {
        String instanceCrn = client.getDefaultClient().createInstance(testDto.getRequest().getName(),
                testContext.get(EnvironmentTestDto.class).getCrn());
        testDto.withCrn(instanceCrn);
        return testDto;
    }
}
