package com.sequenceiq.it.cloudbreak.assertion.freeipa;

import org.assertj.core.api.Assertions;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.it.cloudbreak.FreeIPAClient;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIPAChildEnvironmentTestDto;

public class FreeIpaChildEnvironmentAssertion {

    private FreeIpaChildEnvironmentAssertion() {

    }

    /**
     * Validate that when fetching FreeIpa for the child environment it returns the parent environment's FreeIpa
     */
    public static Assertion<FreeIPAChildEnvironmentTestDto, FreeIPAClient> validate() {
        return (testContext, entity, client) -> {
            DescribeFreeIpaResponse freeIpaResponse = client.getFreeIpaClient().getFreeIpaV1Endpoint()
                    .describe(entity.getRequest().getChildEnvironmentCrn());
            Assertions.assertThat(freeIpaResponse.getEnvironmentCrn()).isEqualTo(entity.getRequest().getParentEnvironmentCrn());
            return entity;
        };
    }
}
