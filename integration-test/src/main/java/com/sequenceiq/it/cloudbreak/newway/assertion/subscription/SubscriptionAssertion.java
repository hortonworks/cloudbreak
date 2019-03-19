package com.sequenceiq.it.cloudbreak.newway.assertion.subscription;

import static org.junit.Assert.assertNotNull;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.util.SubscriptionTestDto;

public class SubscriptionAssertion {

    private SubscriptionAssertion() {
    }

    public static SubscriptionTestDto idExists(TestContext tc, SubscriptionTestDto entity, CloudbreakClient cc) {
        assertNotNull(entity.getResponse().getId());
        return entity;
    }

}
