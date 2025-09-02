package com.sequenceiq.cloudbreak.cloud.aws.common.mapper;

import java.net.SocketTimeoutException;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.inject.Inject;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.aspectj.lang.Signature;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsEncodedAuthorizationFailureMessageDecoder;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.service.Retry.ActionFailedException;
import com.sequenceiq.cloudbreak.util.NullUtil;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.autoscaling.model.ScalingActivityInProgressException;

@Component
public class SdkClientExceptionMapper {

    private static final Pattern MESSAGE_PATTERN = Pattern.compile("(.*) \\(Service:.*, Status Code: .*, Request ID: .*\\)");

    @Inject
    private AwsEncodedAuthorizationFailureMessageDecoder awsEncodedAuthorizationFailureMessageDecoder;

    public RuntimeException map(AwsCredentialView awsCredentialView, String region, SdkException e, Signature signature) {
        String methodName = signature.getName();
        Optional<Throwable> socketTimeoutException = ExceptionUtils.getThrowableList(e)
                .stream()
                .filter(SocketTimeoutException.class::isInstance)
                .findFirst();
        if (socketTimeoutException.isPresent()) {
            String message = addMethodNameIfNotContains("Failed to communicate with AWS provider: " + socketTimeoutException.get().getMessage(), methodName);
            return new ActionFailedException(message);
        }

        String message = awsEncodedAuthorizationFailureMessageDecoder.decodeAuthorizationFailureMessageIfNeeded(awsCredentialView, region, e.getMessage());
        if (!message.equals(e.getMessage())) {
            message = addMethodNameIfNotContains(message, methodName);
            return new CloudConnectorException(message, e);
        }
        if (message.contains("Rate exceeded") || message.contains("Request limit exceeded") || message.contains("Unable to unmarshall response")) {
            message = addMethodNameIfNotContains(message, methodName);
            return new ActionFailedException(message);
        }

        if (e instanceof ScalingActivityInProgressException) {
            message = addMethodNameIfNotContains(message, methodName);
            return new ActionFailedException(message);
        }

        // We use the AwsServiceException to check the cloudformation exists or not. And maybe we built much more logic to this exception.
        // Therefore, the error messages are updated instead of the wrap
        if (e instanceof AwsServiceException ase) {
            return extendErrorMessageWithMethodName(ase, methodName);
        }
        return e;
    }

    private String addMethodNameIfNotContains(String message, String methodName) {
        if (NullUtil.allNotNull(message, methodName)
                && !message.toLowerCase(Locale.ROOT).contains(methodName.toLowerCase(Locale.ROOT))) {
            String pre = "Cannot execute method: " + methodName + ". ";
            return pre + message;
        } else if (message == null && methodName != null) {
            return "Cannot execute method: " + methodName + ". ";
        }
        return message;
    }

    private AwsServiceException extendErrorMessageWithMethodName(AwsServiceException e, String methodName) {
        AwsServiceException.Builder exceptionBuilder = e.toBuilder();
        exceptionBuilder.message(addMethodNameIfNotContains(extractOriginalMessage(e), methodName));
        if (e.awsErrorDetails() != null) {
            AwsErrorDetails.Builder errorDetailsBuilder = e.awsErrorDetails().toBuilder();
            errorDetailsBuilder.errorMessage(addMethodNameIfNotContains(e.awsErrorDetails().errorMessage(), methodName));
            exceptionBuilder.awsErrorDetails(errorDetailsBuilder.build());
        }
        return exceptionBuilder.build();
    }

    private String extractOriginalMessage(AwsServiceException exception) {
        String message = exception.getMessage();
        Matcher matcher = MESSAGE_PATTERN.matcher(message);
        return matcher.find() ? matcher.group(1) : message;
    }
}
