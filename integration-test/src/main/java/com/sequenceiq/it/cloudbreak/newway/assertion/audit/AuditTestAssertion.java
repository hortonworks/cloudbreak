package com.sequenceiq.it.cloudbreak.newway.assertion.audit;

import com.sequenceiq.it.cloudbreak.newway.assertion.AssertionV2;
import com.sequenceiq.it.cloudbreak.newway.dto.audit.AuditTestDto;

public class AuditTestAssertion {

    private AuditTestAssertion() {

    }

    public static AssertionV2<AuditTestDto> listContainsAtLeast(long expectedCount) {
        return (testContext, entity, cloudbreakClient) -> {
            if (entity.getResponses().size() < expectedCount) {
                throw new IllegalArgumentException(String.format("Audit save did not happened.",
                        entity.getName()));
            }
            return entity;
        };
    }
}
