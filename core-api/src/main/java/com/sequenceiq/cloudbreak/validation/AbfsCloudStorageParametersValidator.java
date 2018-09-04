package com.sequenceiq.cloudbreak.validation;

import java.util.regex.Pattern;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.api.model.v2.filesystem.AbfsCloudStorageParameters;

public class AbfsCloudStorageParametersValidator implements ConstraintValidator<ValidAbfsCloudStorageParameters, AbfsCloudStorageParameters> {

    private static final int MIN_ACCOUNT_NAME_LENGTH = 3;

    private static final int MAX_ACCOUNT_NAME_LENGTH = 24;

    private String failMessage = "";

    @Override
    public void initialize(ValidAbfsCloudStorageParameters constraintAnnotation) {
    }

    @Override
    public boolean isValid(AbfsCloudStorageParameters value, ConstraintValidatorContext context) {
        boolean result;
        if (!isAccountNameValid(value.getAccountName())
                || !isAccountKeyValid(value.getAccountKey())) {
            ValidatorUtil.addConstraintViolation(context, failMessage, "status");
            result = false;
        } else {
            result = true;
        }
        return result;
    }

    private boolean isAccountNameValid(String accountName) {
        boolean result;
        if (!isLengthMatches(accountName)) {
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
        if (accountKey == null) {
            failMessage = "Account key should not be null!";
            result = false;
        } else if (!accountKey.matches("^([A-Za-z0-9+/]{4}){21}([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)$")) {
            failMessage = "Must be the base64 encoded representation of 64 random bytes.";
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
