package com.sequenceiq.it.cloudbreak.await;

import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.microservice.MicroserviceClient;

@FunctionalInterface
public interface Await<T extends CloudbreakTestDto, U extends MicroserviceClient> {

    T await(TestContext testContext, T testDto, U client, RunningParameter runningParameter);
}
