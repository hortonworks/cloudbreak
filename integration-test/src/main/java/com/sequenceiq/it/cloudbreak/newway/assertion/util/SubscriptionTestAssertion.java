package com.sequenceiq.it.cloudbreak.newway.assertion.util;

import static org.junit.Assert.assertNotNull;

import com.sequenceiq.it.cloudbreak.newway.assertion.AssertionV2;
import com.sequenceiq.it.cloudbreak.newway.dto.util.SubscriptionTestDto;

public class SubscriptionTestAssertion {

    private SubscriptionTestAssertion() {
    }

    public static AssertionV2<SubscriptionTestDto> idExists() {
        return (testContext, entity, cloudbreakClient) -> {
            assertNotNull(entity.getResponse().getId());
            return entity;
        };
    }

}
