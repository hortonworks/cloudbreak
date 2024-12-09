package com.sequenceiq.cloudbreak.validation;

import jakarta.inject.Inject;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.ws.rs.core.Response.Status.Family;
import jakarta.ws.rs.core.Response.StatusType;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.helper.HttpHelper;
import com.sequenceiq.common.api.util.ValidatorUtil;

@Component
public class HttpContentSizeValidator implements ConstraintValidator<ValidHttpContentSize, String> {

    public static final String FAILED_TO_GET_BY_FAMILY_TYPE = "Failed to get response by the specified URL '%s' due to: '%s'!";

    public static final String INVALID_URL_MSG = "The value should be a valid URL and start with 'http(s)'!";

    public static final String FAILED_TO_GET_WITH_EXCEPTION = "Failed to get response by the specified URL!";

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpContentSizeValidator.class);

    @Inject
    private ContentSizeProvider contentSizeProvider;

    @Inject
    private HttpHelper httpHelper;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        LOGGER.info("content size validation was called. Value: {}", value);

        if (value == null) {
            return true;
        } else if (!value.startsWith("http")) {
            ValidatorUtil.addConstraintViolation(context, INVALID_URL_MSG);
            return false;
        }
        try {
            Pair<StatusType, Integer> contentLength = httpHelper.getContentLength(value);
            if (!contentLength.getKey().getFamily().equals(Family.SUCCESSFUL)) {
                String msg = String.format(FAILED_TO_GET_BY_FAMILY_TYPE, value, contentLength.getKey().getReasonPhrase());
                ValidatorUtil.addConstraintViolation(context, msg);
                return false;
            }
            int maxSizeInBytes = contentSizeProvider.getMaxSizeInBytes();
            boolean valid = true;

            if (contentLength.getValue() == null || contentLength.getValue() < 0) {
                logAndAddViolation(context, "The content length of the image catalog URL is not returned by the server!");
                valid = false;
            } else if (contentLength.getValue() == 0) {
                logAndAddViolation(context, "The content length of the image catalog URL is 0!");
                valid = false;
            } else if (contentLength.getValue() > maxSizeInBytes) {
                String message = String.format("The content of the image catalog URL must be less than {%s}", FileUtils.byteCountToDisplaySize(maxSizeInBytes));
                logAndAddViolation(context, message);
                valid = false;
            }

            return valid;
        } catch (Throwable throwable) {
            LOGGER.info("content size validation failed.", throwable);
            ValidatorUtil.addConstraintViolation(context, FAILED_TO_GET_WITH_EXCEPTION);
        }
        return false;
    }

    private void logAndAddViolation(ConstraintValidatorContext context, String message) {
        LOGGER.info(message);
        ValidatorUtil.addConstraintViolation(context, message);
    }
}
