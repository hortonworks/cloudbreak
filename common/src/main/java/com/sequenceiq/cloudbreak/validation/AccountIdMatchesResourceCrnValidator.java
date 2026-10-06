package com.sequenceiq.cloudbreak.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.common.api.util.ValidatorUtil;

/**
 * Cross-field check: {@code account_id} must equal the account segment of {@code resource_crn}.
 * Intended to run in {@link AccountIdMatchesResourceCrnGroup} after field constraints on the same bean succeed
 * (via {@link jakarta.validation.GroupSequence}). Absent or unparseable values are left to those field constraints
 * ({@code @NotBlank}, {@code @ValidCrn}) rather than reported here, so this stays safe wherever it is wired.
 */
public class AccountIdMatchesResourceCrnValidator implements ConstraintValidator<AccountIdMatchesResourceCrn, Object> {

    private String accountIdProperty;

    private String resourceCrnProperty;

    @Override
    public void initialize(AccountIdMatchesResourceCrn constraintAnnotation) {
        accountIdProperty = constraintAnnotation.accountIdProperty();
        resourceCrnProperty = constraintAnnotation.resourceCrnProperty();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        BeanWrapper wrapper = new BeanWrapperImpl(value);
        String accountId = (String) wrapper.getPropertyValue(accountIdProperty);
        String resourceCrn = (String) wrapper.getPropertyValue(resourceCrnProperty);
        if (StringUtils.isBlank(accountId) || !Crn.isCrn(resourceCrn)) {
            return true;
        }
        if (accountId.equals(Crn.safeFromString(resourceCrn).getAccountId())) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        ValidatorUtil.addConstraintViolation(context, String.format(
                "account_id '%s' does not match the account in resource_crn '%s'", accountId, resourceCrn),
                accountIdProperty);
        return false;
    }
}
