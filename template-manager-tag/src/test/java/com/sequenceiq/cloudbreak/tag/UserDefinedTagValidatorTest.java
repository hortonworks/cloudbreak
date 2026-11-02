package com.sequenceiq.cloudbreak.tag;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.TagKeyNormalizer;
import com.sequenceiq.cloudbreak.validation.ValidationResult;

class UserDefinedTagValidatorTest {

    private static final String AWS = "AWS";

    private final UserDefinedTagValidator underTest = new UserDefinedTagValidator();

    @Test
    void validateAgainstDefaultTagsShouldPassWhenNoConflict() {
        ValidationResult result = underTest.validateAgainstDefaultTags(
                Map.of("customKey", "customValue"),
                Map.of("owner", "john doe", "Cloudera-Resource-Name", "resourceName"));

        assertThat(result.hasError()).isFalse();
    }

    @Test
    void validateAgainstDefaultTagsShouldFailWhenKeyConflictsWithDefaultTag() {
        ValidationResult result = underTest.validateAgainstDefaultTags(
                Map.of("owner", "attacker", "customKey", "customValue"),
                Map.of("owner", "john doe"));

        assertThat(result.hasError()).isTrue();
        assertThat(result.getFormattedErrors()).contains("owner");
        assertThat(result.getFormattedErrors()).contains("Default tags cannot be overridden by user-defined tags");
    }

    @Test
    void validateAgainstDefaultTagsShouldPassWhenUserDefinedTagsAreEmpty() {
        ValidationResult result = underTest.validateAgainstDefaultTags(Map.of(), Map.of("owner", "john doe"));

        assertThat(result.hasError()).isFalse();
    }

    @Test
    void validateAgainstDefaultTagsShouldPassWhenDefaultTagsAreEmpty() {
        ValidationResult result = underTest.validateAgainstDefaultTags(Map.of("customKey", "customValue"), Map.of());

        assertThat(result.hasError()).isFalse();
    }

    @Test
    void validateTagKeysToRemoveShouldFailWhenEmpty() {
        ValidationResult result = underTest.validateTagKeysToRemove(Set.of(), Map.of("owner", "john doe"), Map.of(), AWS, TagKeyNormalizer.IDENTITY);

        assertThat(result.hasError()).isTrue();
        assertThat(result.getFormattedErrors()).contains("must not be empty");
    }

    @Test
    void validateTagKeysToRemoveShouldFailWhenDefaultTagKeyRequested() {
        ValidationResult result = underTest.validateTagKeysToRemove(
                Set.of("owner", "custom"),
                Map.of("owner", "john doe"),
                Map.of(),
                AWS,
                TagKeyNormalizer.IDENTITY);

        assertThat(result.hasError()).isTrue();
        assertThat(result.getFormattedErrors()).contains("default");
        assertThat(result.getFormattedErrors()).contains("owner");
    }

    @Test
    void validateTagKeysToRemoveShouldFailWhenApplicationTagKeyRequested() {
        ValidationResult result = underTest.validateTagKeysToRemove(
                Set.of("application"),
                Map.of(),
                Map.of("application", "app"),
                AWS,
                TagKeyNormalizer.IDENTITY);

        assertThat(result.hasError()).isTrue();
        assertThat(result.getFormattedErrors()).contains("application");
    }

    @Test
    void validateTagKeysToRemoveShouldPassForUserDefinedKey() {
        ValidationResult result = underTest.validateTagKeysToRemove(
                Set.of("custom"),
                Map.of("owner", "john doe"),
                Map.of("application", "app"),
                AWS,
                TagKeyNormalizer.IDENTITY);

        assertThat(result.hasError()).isFalse();
    }

    @Test
    void validateTagKeysToRemoveShouldFailWhenNormalizedKeyMatchesProtectedDefaultTag() {
        TagKeyNormalizer lowerCaseNormalizer = key -> key.toLowerCase();

        ValidationResult result = underTest.validateTagKeysToRemove(
                Set.of("cloudera-resource-name"),
                Map.of("Cloudera-Resource-Name", "resourceName"),
                Map.of(),
                AWS,
                lowerCaseNormalizer);

        assertThat(result.hasError()).isTrue();
        assertThat(result.getFormattedErrors()).contains("default");
        assertThat(result.getFormattedErrors()).contains("cloudera-resource-name");
    }

    @Test
    void validateTagKeysToRemoveShouldFailWhenAwsReservedKeyRequestedOnAws() {
        ValidationResult result = underTest.validateTagKeysToRemove(
                Set.of("AWS:cloudformation:stack-name", "custom"),
                Map.of(),
                Map.of(),
                AWS,
                TagKeyNormalizer.IDENTITY);

        assertThat(result.hasError()).isTrue();
        assertThat(result.getFormattedErrors()).contains("AWS:cloudformation:stack-name");
        assertThat(result.getFormattedErrors()).contains("reserved by AWS");
        assertThat(result.getFormattedErrors()).doesNotContain("custom");
    }

    @Test
    void validateTagKeysToRemoveShouldPassForAwsPrefixedKeyOnOtherPlatform() {
        ValidationResult result = underTest.validateTagKeysToRemove(
                Set.of("aws:custom"),
                Map.of(),
                Map.of(),
                "AZURE",
                TagKeyNormalizer.IDENTITY);

        assertThat(result.hasError()).isFalse();
    }
}
