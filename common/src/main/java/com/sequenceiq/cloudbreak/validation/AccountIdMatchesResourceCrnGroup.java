package com.sequenceiq.cloudbreak.validation;

/**
 * Validation group for {@link AccountIdMatchesResourceCrn}. Use with {@link jakarta.validation.GroupSequence} on the
 * same bean so this runs only after default-group field constraints ({@code @NotBlank}, {@code @ValidCrn}, etc.) pass.
 */
public interface AccountIdMatchesResourceCrnGroup {
}
