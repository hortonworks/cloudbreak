package com.sequenceiq.it.cloudbreak.newway.assertion;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.CloudbreakEntity;

public class AssertStatusReasonMessage<T extends CloudbreakEntity> implements AssertionV2<T> {

    private String message;

    public AssertStatusReasonMessage(String message) {
        this.message = message;
    }

    @Override
    public T doAssertion(TestContext testContext, T entity, CloudbreakClient cloudbreakClient) {
        if (testContext.getStatus().isPresent() && !message.equals(testContext.getStatus().get().getRight())) {
            throw new IllegalStateException("statusReason is mismatch: actual: " + testContext.getStatus().get().getRight());
        }
        return entity;
    }
}
