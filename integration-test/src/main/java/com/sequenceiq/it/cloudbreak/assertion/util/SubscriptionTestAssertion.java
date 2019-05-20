package com.sequenceiq.it.cloudbreak.assertion.util;

import static org.junit.Assert.assertNotNull;

import com.sequenceiq.it.cloudbreak.dto.util.SubscriptionTestDto;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;

public class SubscriptionTestAssertion {

    private SubscriptionTestAssertion() {
    }

    public static Assertion<SubscriptionTestDto> idExists() {
        return (testContext, entity, cloudbreakClient) -> {
            assertNotNull(entity.getResponse().getId());
            return entity;
        };
    }

}
