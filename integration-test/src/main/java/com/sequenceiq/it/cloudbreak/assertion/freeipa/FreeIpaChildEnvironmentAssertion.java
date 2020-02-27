package com.sequenceiq.it.cloudbreak.assertion.freeipa;

import javax.ws.rs.NotFoundException;

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
    public static Assertion<FreeIPAChildEnvironmentTestDto, FreeIPAClient> validateChildFreeipa() {
        return (testContext, entity, client) -> {
            DescribeFreeIpaResponse freeIpaResponse = describeChildFreeipa(entity, client);
            Assertions.assertThat(freeIpaResponse.getEnvironmentCrn())
                    .isEqualTo(entity.getRequest().getParentEnvironmentCrn());
            return entity;
        };
    }

    /**
     * Validate that when fetching FreeIpa for the child environment it is not found
     */
    public static Assertion<FreeIPAChildEnvironmentTestDto, FreeIPAClient> validateNoFreeipa() {
        return (testContext, entity, client) -> {
            Assertions.assertThatThrownBy(() -> describeChildFreeipa(entity, client))
                    .isInstanceOf(NotFoundException.class);
            return entity;
        };
    }

    private static DescribeFreeIpaResponse describeChildFreeipa(FreeIPAChildEnvironmentTestDto entity, FreeIPAClient client) {
        return client.getFreeIpaClient().getFreeIpaV1Endpoint()
                .describe(entity.getRequest().getChildEnvironmentCrn());
    }
}
