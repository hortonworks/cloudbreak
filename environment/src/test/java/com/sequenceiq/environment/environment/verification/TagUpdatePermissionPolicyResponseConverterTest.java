package com.sequenceiq.environment.environment.verification;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.sequenceiq.environment.api.v1.environment.model.response.PolicyValidationErrorResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.PolicyValidationErrorResponses;

class TagUpdatePermissionPolicyResponseConverterTest {

    @Test
    void grantedProducesAnEmptyCollection() {
        PolicyValidationErrorResponses result = TagUpdatePermissionPolicyResponseConverter.convert(null, List.of());

        assertThat(result.getResponses()).isEmpty();
    }

    @Test
    void everyFailedActionBecomesItsOwnRow() {
        List<String> failedActions = List.of("ec2:CreateTags : *", "kms:TagResource : *");

        PolicyValidationErrorResponses result = TagUpdatePermissionPolicyResponseConverter.convert("missing actions", failedActions);

        assertThat(result.getResponses()).hasSize(2);
        assertThat(result.getResponses()).allSatisfy(row -> {
            assertThat(row.getService()).isEqualTo("TAG_UPDATE");
            assertThat(row.getCode()).isEqualTo(404);
        });
        assertThat(result.getResponses().stream().map(PolicyValidationErrorResponse::getMessage))
                .containsExactlyInAnyOrderElementsOf(failedActions);
    }

    @Test
    void aFailureWithNoNamedActionIsNotRenderedAsGranted() {
        PolicyValidationErrorResponses result = TagUpdatePermissionPolicyResponseConverter.convert(
                "Failed to verify the tag-update permissions of CDP Credential 'cred-1'", List.of());

        assertThat(result.getResponses())
                .withFailMessage("an unverifiable credential must not look like a verified one, which an empty collection would mean")
                .hasSize(1);
        PolicyValidationErrorResponse row = result.getResponses().iterator().next();
        assertThat(row.getService()).isEqualTo("TAG_UPDATE");
        assertThat(row.getMessage()).isEqualTo("Failed to verify the tag-update permissions of CDP Credential 'cred-1'");
        assertThat(row.getCode()).isEqualTo(503);
    }
}
