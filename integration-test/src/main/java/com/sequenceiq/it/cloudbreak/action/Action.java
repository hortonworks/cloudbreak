package com.sequenceiq.it.cloudbreak.action;

import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.microservice.MicroserviceClient;

@FunctionalInterface
public interface Action<T extends CloudbreakTestDto, U extends MicroserviceClient> {

    T action(TestContext testContext, T testDto, U client) throws Exception;
}
