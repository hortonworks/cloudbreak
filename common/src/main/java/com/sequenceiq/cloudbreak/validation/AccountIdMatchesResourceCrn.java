package com.sequenceiq.cloudbreak.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Ensures {@code account_id} matches the account embedded in {@code resource_crn}. Declare it with
 * {@code groups = AccountIdMatchesResourceCrnGroup.class} and pair that with {@link jakarta.validation.GroupSequence} on
 * the bean, so this runs only after default-group field constraints ({@code @NotBlank}, {@code @NotEmpty},
 * {@code @ValidCrn}) succeed.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AccountIdMatchesResourceCrnValidator.class)
public @interface AccountIdMatchesResourceCrn {

    String accountIdProperty() default "accountId";

    String resourceCrnProperty() default "resourceCrn";

    String message() default "account_id does not match the account in resource_crn";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default {};
}
