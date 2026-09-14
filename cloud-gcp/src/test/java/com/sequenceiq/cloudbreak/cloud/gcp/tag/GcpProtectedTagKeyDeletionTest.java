package com.sequenceiq.cloudbreak.cloud.gcp.tag;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpLabelUtil;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.tag.UserDefinedTagValidator;
import com.sequenceiq.cloudbreak.validation.ValidationResult;

class GcpProtectedTagKeyDeletionTest {

    private final GcpLabelUtil gcpLabelUtil = new GcpLabelUtil();

    private final CloudPlatformTagKeyNormalizerProvider tagKeyNormalizerProvider = new CloudPlatformTagKeyNormalizerProvider(gcpLabelUtil);

    private final UserDefinedTagValidator userDefinedTagValidator = new UserDefinedTagValidator();

    @Test
    void validateTagKeysToRemoveShouldRejectGcpNormalizedProtectedDefaultTag() {
        ValidationResult result = userDefinedTagValidator.validateTagKeysToRemove(
                Set.of("cloudera-resource-name"),
                Map.of("Cloudera-Resource-Name", "resourceName"),
                Map.of(),
                tagKeyNormalizerProvider.forPlatform("GCP"));

        assertThat(result.hasError()).isTrue();
        assertThat(result.getFormattedErrors()).contains("default");
    }

    @Test
    void getUserDefinedTagKeysWithoutProtectedTagsShouldFilterGcpNormalizedProtectedDefaultTag() {
        StackTags stackTags = new StackTags(Map.of(), Map.of(), Map.of("Cloudera-Resource-Name", "resourceName"));

        assertThat(stackTags.getUserDefinedTagKeysWithoutProtectedTags(
                Set.of("cloudera-resource-name"),
                tagKeyNormalizerProvider.forPlatform("GCP"))).isEmpty();
    }
}
