package com.sequenceiq.it.cloudbreak.action.idbmms;

import java.util.Optional;

import com.sequenceiq.it.cloudbreak.IdbmmsClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.idbmms.IdbmmsTestDto;

public class IdbmmsDeleteAction implements Action<IdbmmsTestDto, IdbmmsClient> {

    @Override
    public IdbmmsTestDto action(TestContext testContext, IdbmmsTestDto testDto, IdbmmsClient client) throws Exception {
        String userCrn = testContext.getActingUserCrn().toString();
        String environmentCrn = testContext.get(EnvironmentTestDto.class).getCrn();
        client.getDefaultClient()
                .deleteMappings(userCrn, environmentCrn, Optional.empty());
        return testDto;
    }
}
