package com.sequenceiq.it.cloudbreak.action;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;

@FunctionalInterface
public interface Action<T extends CloudbreakTestDto> {

    T action(TestContext testContext, T testDto, CloudbreakClient cloudbreakClient) throws Exception;
}
