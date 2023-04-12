package com.sequenceiq.cloudbreak.validation;

import javax.inject.Inject;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.core.Response.StatusType;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.helper.HttpHelper;

@Component
public class HttpContentSizeValidator implements ConstraintValidator<ValidHttpContentSize, String> {

    public static final String FAILED_TO_GET_BY_FAMILY_TYPE = "Failed to get response by the specified URL '%s' due to: '%s'!";

    public static final String INVALID_URL_MSG = "The value should be a valid URL and start with 'http(s)'!";

    public static final String FAILED_TO_GET_WITH_EXCEPTION = "Failed to get response by the specified URL!";

    public static final String MESSAGE_VARIABLE = "contentLimit";

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpContentSizeValidator.class);

    @Inject
    private ContentSizeProvider contentSizeProvider;

    private HttpHelper httpHelper = HttpHelper.getInstance();

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        LOGGER.info("content size validation was called. Value: {}", value);

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
            int maxSizeInBytes = contentSizeProvider.getMaxSizeInBytes();
            boolean valid = contentLength.getValue() > 0 && contentLength.getValue() <= maxSizeInBytes;
            if (!valid) {
                context.unwrap(HibernateConstraintValidatorContext.class)
                        .addMessageParameter(MESSAGE_VARIABLE, FileUtils.byteCountToDisplaySize(maxSizeInBytes));
            }

            return valid;
        } catch (Throwable throwable) {
            LOGGER.info("content size validation failed.", throwable);
            context.buildConstraintViolationWithTemplate(FAILED_TO_GET_WITH_EXCEPTION).addConstraintViolation();
        }
        return false;
    }
}