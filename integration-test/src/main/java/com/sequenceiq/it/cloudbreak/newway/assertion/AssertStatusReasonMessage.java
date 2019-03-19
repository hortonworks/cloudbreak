package com.sequenceiq.it.cloudbreak.newway.assertion;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.CloudbreakTestDto;

public class AssertStatusReasonMessage<T extends CloudbreakTestDto> implements AssertionV2<T> {

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
