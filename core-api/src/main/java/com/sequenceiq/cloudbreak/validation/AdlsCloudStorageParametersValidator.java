package com.sequenceiq.cloudbreak.validation;

import com.sequenceiq.cloudbreak.api.model.v2.filesystem.AdlsCloudStorageParameters;
import org.apache.commons.lang3.StringUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class AdlsCloudStorageParametersValidator implements ConstraintValidator<ValidAdlsCloudStorageParameters, AdlsCloudStorageParameters> {

    private static final int MIN_ACCOUNT_NAME_LENGTH = 3;

    private static final int MAX_ACCOUNT_NAME_LENGTH = 24;

    private static final String PROPERTY_MODEL_VALUE = "status";

    private String failMessage = "";

    @Override
    public void initialize(ValidAdlsCloudStorageParameters constraintAnnotation) {
    }

    @Override
    public boolean isValid(AdlsCloudStorageParameters value, ConstraintValidatorContext context) {
        boolean result;
        if (!isAccountNameValid(value.getAccountName())) {
            ValidatorUtil.addConstraintViolation(context, failMessage, PROPERTY_MODEL_VALUE);
            result = false;
        } else if (value.getTenantId() == null) {
            ValidatorUtil.addConstraintViolation(context, "Tenant ID should not be null!", PROPERTY_MODEL_VALUE);
            result = false;
        } else {
            result = true;
        }
        return result;
    }

    private boolean isAccountNameValid(String accountName) {
        boolean result;
        if (!isLengthMatches(accountName)) {
            failMessage = String.format("Account name value's length must be in the range of min %d and max %d characters. Current: %d",
                    MIN_ACCOUNT_NAME_LENGTH,
                    MAX_ACCOUNT_NAME_LENGTH,
                    StringUtils.length(accountName));
            result = false;
        } else if (!accountName.matches("^[a-z0-9]$")) {
            failMessage = "Account name must contain only numbers and lowercase letters";
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
