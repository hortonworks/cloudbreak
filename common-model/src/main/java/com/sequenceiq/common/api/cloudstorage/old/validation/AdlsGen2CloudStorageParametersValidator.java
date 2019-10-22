package com.sequenceiq.common.api.cloudstorage.old.validation;

import java.util.Objects;
import java.util.regex.Pattern;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.util.ValidatorUtil;

public class AdlsGen2CloudStorageParametersValidator implements ConstraintValidator<ValidAdlsGen2CloudStorageParameters, AdlsGen2CloudStorageV1Parameters> {

    private static final int MIN_ACCOUNT_NAME_LENGTH = 3;

    private static final int MAX_ACCOUNT_NAME_LENGTH = 24;

    private String failMessage = "";

    @Override
    public void initialize(ValidAdlsGen2CloudStorageParameters constraintAnnotation) {
    }

    @Override
    public boolean isValid(AdlsGen2CloudStorageV1Parameters value, ConstraintValidatorContext context) {
        boolean result;
        if (!isAccountNameValid(value.getAccountName())
                || !isAccountKeyValid(value.getAccountKey())
                || !isManagedIdentityValid(value.getManagedIdentity())
                || !isOnlyOneAuthenticationMethodUsed(value)) {
            ValidatorUtil.addConstraintViolation(context, failMessage, "status");
            result = false;
        } else {
            result = true;
        }
        return result;
    }

    private boolean isOnlyOneAuthenticationMethodUsed(AdlsGen2CloudStorageV1Parameters value) {
        boolean result;
        if ((Objects.isNull(value.getManagedIdentity()) && Objects.nonNull(value.getAccountKey()) && Objects.nonNull(value.getAccountName()))
            || (Objects.nonNull(value.getManagedIdentity()) && Objects.isNull(value.getAccountKey()) && Objects.isNull(value.getAccountName()))) {
            result = true;
        } else {
            failMessage = "ADLS Gen 2 account should have only one authentication method, either managed identity or account key with account name";
            result = false;
        }
        return result;
    }

    private boolean isAccountNameValid(String accountName) {
        boolean result;
        if (Objects.isNull(accountName)) {
            result = true;
        } else if (!isLengthMatches(accountName)) {
            failMessage = String.format("Account name cannot be null and it's value must be in the range of min %d and max %d characters. Current: %d",
                    MIN_ACCOUNT_NAME_LENGTH,
                    MAX_ACCOUNT_NAME_LENGTH,
                    StringUtils.length(accountName));
            result = false;
        } else if (!Pattern.matches("^[a-z0-9]*", accountName)) {
            failMessage = "Account name must contain only numbers and lowercase letters";
            result = false;
        } else {
            result = true;
        }
        return result;
    }

    private boolean isAccountKeyValid(String accountKey) {
        boolean result;
        if (Objects.isNull(accountKey)) {
            result = true;
        } else if (!accountKey.matches("^([A-Za-z0-9+/]{4}){21}([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)$")) {
            failMessage = "Must be the base64 encoded representation of 64 random bytes.";
            result = false;
        } else {
            result = true;
        }
        return result;
    }

    private boolean isManagedIdentityValid(String managedIdentity) {
        boolean result;
        if (Objects.isNull(managedIdentity)) {
            result = true;
        } else if (!managedIdentity.matches("^/subscriptions/[0-9a-f]{8}-[0-9a-f]{4}-[0-5][0-9a-f]{3}-[089ab][0-9a-f]{3}-[0-9a-f]{12}/"
                + "resourceGroups/[-\\w._()]+/providers/Microsoft.ManagedIdentity/userAssignedIdentities/[A-Za-z0-9-_]*$")) {
            failMessage = "Must be a full valid managed identity id.";
            result = false;
        } else {
            result = true;
        }
        return result;
    }

    private boolean isLengthMatches(String text) {
        return StringUtils.length(text) >= MIN_ACCOUNT_NAME_LENGTH && StringUtils.length(text) <= MAX_ACCOUNT_NAME_LENGTH;
    }

}
