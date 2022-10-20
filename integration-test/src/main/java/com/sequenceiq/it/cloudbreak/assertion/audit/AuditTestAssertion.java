package com.sequenceiq.it.cloudbreak.assertion.audit;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.dto.audit.AuditTestDto;

public class AuditTestAssertion {

    private AuditTestAssertion() {

    }

    public static Assertion<AuditTestDto, CloudbreakClient> listContainsAtLeast(long expectedCount) {
        return (testContext, entity, cloudbreakClient) -> {
            if (entity.getResponses().size() < expectedCount) {
                throw new IllegalArgumentException(String.format("Audit save did not happened.",
                        entity.getName()));
            }
            return entity;
        };
    }
}
