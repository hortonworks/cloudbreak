package com.sequenceiq.it.cloudbreak.assertion;

import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class AssertStatusReasonMessage<T extends CloudbreakTestDto> implements Assertion<T, CloudbreakClient> {

    private String message;

    public AssertStatusReasonMessage(String message) {
        this.message = message;
    }

    @Override
    public T doAssertion(TestContext testContext, T testDto, CloudbreakClient cloudbreakClient) {
        String statusReason = testContext.getStatuses().get("statusReason");
        if (!message.equals(statusReason)) {
            throw new IllegalStateException("statusReason is mismatch: actual: " + statusReason);
        }
        return testDto;
    }
}
