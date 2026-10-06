package com.sequenceiq.cloudbreak.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.GroupSequence;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;

class AccountIdMatchesResourceCrnValidatorTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void passesWhenAccountIdMatchesResourceCrn() {
        DispatchBody body = validBody();
        body.setAccountId("acc-12345");
        body.setResourceCrn("crn:cdp:datahub:us-west-1:acc-12345:cluster:my-dh");

        assertThat(validator.validate(body)).isEmpty();
    }

    @Test
    void failsWhenAccountIdDoesNotMatchResourceCrn() {
        DispatchBody body = validBody();
        body.setAccountId("other-account");
        body.setResourceCrn("crn:cdp:datahub:us-west-1:acc-12345:cluster:my-dh");

        Set<ConstraintViolation<DispatchBody>> violations = validator.validate(body);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("account_id");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("accountId");
    }

    @Test
    void doesNotRunAccountMatchWhenResourceCrnIsInvalid() {
        DispatchBody body = validBody();
        body.setAccountId("acc-12345");
        body.setResourceCrn("not-a-crn");

        Set<ConstraintViolation<DispatchBody>> violations = validator.validate(body);

        assertThat(violations).noneMatch(v -> v.getMessage().contains("does not match the account"));
    }

    @Test
    void doesNotRunAccountMatchWhenAccountIdIsBlank() {
        DispatchBody body = validBody();
        body.setAccountId("   ");
        body.setResourceCrn("crn:cdp:datahub:us-west-1:acc-12345:cluster:my-dh");

        Set<ConstraintViolation<DispatchBody>> violations = validator.validate(body);

        assertThat(violations).noneMatch(v -> v.getMessage().contains("does not match the account"));
    }

    /**
     * The group sequence normally shields the cross-field check behind {@code @NotBlank}/{@code @ValidCrn}. Validating
     * its group on its own proves the validator is safe wherever it is wired: blank {@code account_id} and non-CRN
     * {@code resource_crn} are skipped via {@code Crn.isCrn(...)} before {@code Crn.safeFromString(...)}, so a missing
     * guard would risk an NPE (500) instead of a field violation (400).
     */
    @Test
    void toleratesMissingAndUnparseableValuesWhenRunOutsideTheGroupSequence() {
        DispatchBody blank = new DispatchBody();

        assertThat(validator.validate(blank, AccountIdMatchesResourceCrnGroup.class)).isEmpty();

        DispatchBody malformed = new DispatchBody();
        malformed.setAccountId("acc-12345");
        malformed.setResourceCrn("not-a-crn");

        assertThat(validator.validate(malformed, AccountIdMatchesResourceCrnGroup.class)).isEmpty();
    }

    private static DispatchBody validBody() {
        return new DispatchBody();
    }

    @GroupSequence({DispatchBody.class, AccountIdMatchesResourceCrnGroup.class})
    @AccountIdMatchesResourceCrn(groups = AccountIdMatchesResourceCrnGroup.class)
    private static final class DispatchBody {

        @NotBlank
        private String accountId;

        @NotEmpty
        @ValidCrn(resource = CrnResourceDescriptor.DATAHUB)
        private String resourceCrn;

        public String getAccountId() {
            return accountId;
        }

        public void setAccountId(String accountId) {
            this.accountId = accountId;
        }

        public String getResourceCrn() {
            return resourceCrn;
        }

        public void setResourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
        }
    }
}
