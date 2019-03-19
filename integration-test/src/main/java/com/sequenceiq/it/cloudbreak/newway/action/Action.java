package com.sequenceiq.it.cloudbreak.newway.action;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.CloudbreakTestDto;

@FunctionalInterface
public interface Action<T extends CloudbreakTestDto> {

    T action(TestContext testContext, T testDto, CloudbreakClient cloudbreakClient) throws Exception;
}
