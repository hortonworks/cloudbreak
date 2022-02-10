package com.sequenceiq.cloudbreak.cloud.aws.common.mapper;

import javax.inject.Inject;

import org.aspectj.lang.Signature;
import org.springframework.stereotype.Component;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.autoscaling.model.ScalingActivityInProgressException;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsEncodedAuthorizationFailureMessageDecoder;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.service.Retry.ActionFailedException;
import com.sequenceiq.cloudbreak.util.NullUtil;

@Component
public class SdkClientExceptionMapper {

    @Inject
    private AwsEncodedAuthorizationFailureMessageDecoder awsEncodedAuthorizationFailureMessageDecoder;

    public RuntimeException map(AwsCredentialView awsCredentialView, String region, SdkClientException e, Signature signature) {
        String message = awsEncodedAuthorizationFailureMessageDecoder.decodeAuthorizationFailureMessageIfNeeded(awsCredentialView, region, e.getMessage());
        String methodName = signature.getName();
        if (!message.equals(e.getMessage())) {
            message = addMethodNameIfNotContains(message, methodName);
            return new CloudConnectorException(message, e);
        }
        if (message.contains("Rate exceeded") || message.contains("Request limit exceeded")) {
            message = addMethodNameIfNotContains(message, methodName);
            return new ActionFailedException(message);
        }

        if (e instanceof ScalingActivityInProgressException) {
            message = addMethodNameIfNotContains(message, methodName);
            return new ActionFailedException(message);
        }
        // We use the AmazonServiceException to check the cloudformation exists or not. And maybe we built much more logic to this exception.
        // Therefore the error messages are updated instead of the wrap
        if (e instanceof AmazonServiceException) {
            extendErrorMessageWithMethodName((AmazonServiceException) e, methodName);
        }
        return e;
    }

    private String addMethodNameIfNotContains(String message, String methodName) {
        if (NullUtil.allNotNull(message, methodName) && !message.toLowerCase().contains(methodName.toLowerCase())) {
            String pre = "Cannot execute method: " + methodName + ". ";
            return pre + message;
        } else if (message == null && methodName != null) {
            return "Cannot execute method: " + methodName + ". ";
        }
        return message;
    }

    private void extendErrorMessageWithMethodName(AmazonServiceException e, String methodName) {
        e.setErrorMessage(addMethodNameIfNotContains(e.getErrorMessage(), methodName));
    }
}
