package com.sequenceiq.cloudbreak.cloud.aws.mapper;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.amazonaws.SdkClientException;
import com.sequenceiq.cloudbreak.cloud.aws.util.AwsEncodedAuthorizationFailureMessageDecoder;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.service.Retry.ActionFailedException;

@Component
public class SdkClientExceptionMapper {

    @Inject
    private AwsEncodedAuthorizationFailureMessageDecoder awsEncodedAuthorizationFailureMessageDecoder;

    public RuntimeException map(AwsCredentialView awsCredentialView, String region, SdkClientException e) {
        String message = awsEncodedAuthorizationFailureMessageDecoder.decodeAuthorizationFailureMessageIfNeeded(awsCredentialView, region, e.getMessage());
        if (!message.equals(e.getMessage())) {
            return new CloudConnectorException(message, e);
        }
        if (message.contains("Rate exceeded") || message.contains("Request limit exceeded")) {
            return new ActionFailedException(message);
        }
        return e;
    }
}
