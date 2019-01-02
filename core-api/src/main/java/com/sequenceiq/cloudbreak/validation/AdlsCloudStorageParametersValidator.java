package com.sequenceiq.cloudbreak.validation;

import java.util.regex.Pattern;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.AdlsCloudStorageV4Parameters;

public class AdlsCloudStorageParametersValidator implements ConstraintValidator<ValidAdlsCloudStorageParameters, AdlsCloudStorageV4Parameters> {

    private static final int MIN_ACCOUNT_NAME_LENGTH = 3;

    private static final int MAX_ACCOUNT_NAME_LENGTH = 24;

    private static final String PROPERTY_MODEL_VALUE = "status";

    private String failMessage = "";

    @Override
    public void initialize(ValidAdlsCloudStorageParameters constraintAnnotation) {
    }

    @Override
    public boolean isValid(AdlsCloudStorageV4Parameters value, ConstraintValidatorContext context) {
        boolean result = false;
        if (!isAccountNameValid(value.getAccountName())) {
            ValidatorUtil.addConstraintViolation(context, failMessage, PROPERTY_MODEL_VALUE);
        } else if (isClientIdOrCredentialEmpty(value)) {
            ValidatorUtil.addConstraintViolation(context, "Client ID and Credential should be fill together only!", PROPERTY_MODEL_VALUE);
        } else {
            result = true;
        }
        return result;
    }

    private boolean isClientIdOrCredentialEmpty(AdlsCloudStorageV4Parameters value) {
        boolean clientIdEmpty = StringUtils.isNoneEmpty(value.getClientId());
        boolean credentialEmpty = StringUtils.isNoneEmpty(value.getCredential());
        return clientIdEmpty != credentialEmpty;
    }

    private boolean isAccountNameValid(String accountName) {
        boolean result;
        if (!isLengthMatches(accountName)) {
            failMessage = String.format("Account name value's length must be in the range of min %d and max %d characters. Current: %d",
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

    private boolean isLengthMatches(String text) {
        int length = StringUtils.length(text);
        return length >= MIN_ACCOUNT_NAME_LENGTH && length <= MAX_ACCOUNT_NAME_LENGTH;
    }

}