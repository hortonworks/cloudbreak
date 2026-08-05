package com.sequenceiq.cloudbreak.tag;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.validation.ValidationResult;

class UserDefinedTagValidatorTest {

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
}
