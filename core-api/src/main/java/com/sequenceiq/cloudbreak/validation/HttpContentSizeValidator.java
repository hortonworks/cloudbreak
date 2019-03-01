package com.sequenceiq.cloudbreak.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.core.Response.StatusType;

import org.apache.commons.lang3.tuple.Pair;

import com.sequenceiq.cloudbreak.api.helper.HttpHelper;

public class HttpContentSizeValidator implements ConstraintValidator<ValidHttpContentSize, String> {

    public static final int MAX_SIZE = 5;

    public static final int MAX_IN_BYTES = 5 * 1024 * 1024;

    public static final String FAILED_TO_GET_BY_FAMILY_TYPE = "Failed to get response by the specified URL '%s' due to: '%s'!";

    public static final String INVALID_URL_MSG = "The value should be a valid URL and start with 'http(s)'!";

    public static final String FAILED_TO_GET_WITH_EXCEPTION = "Failed to get response by the specified URL!";

    private HttpHelper httpHelper = HttpHelper.getInstance();

    @Override
    public void initialize(ValidHttpContentSize constraintAnnotation) {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        } else if (!value.startsWith("http")) {
            context.buildConstraintViolationWithTemplate(INVALID_URL_MSG).addConstraintViolation();
            return false;
        }
        try {
            Pair<StatusType, Integer> contentLength = httpHelper.getContentLength(value);
            if (!contentLength.getKey().getFamily().equals(Family.SUCCESSFUL)) {
                String msg = String.format(FAILED_TO_GET_BY_FAMILY_TYPE, value, contentLength.getKey().getReasonPhrase());
                context.buildConstraintViolationWithTemplate(msg).addConstraintViolation();
                return false;
            }
            return contentLength.getValue() > 0 && contentLength.getValue() <= MAX_IN_BYTES;
        } catch (Throwable throwable) {
            context.buildConstraintViolationWithTemplate(FAILED_TO_GET_WITH_EXCEPTION).addConstraintViolation();
        }
        return false;
    }
}