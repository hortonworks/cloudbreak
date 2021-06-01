package com.sequenceiq.cloudbreak.cloud.aws.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.amazonaws.services.securitytoken.model.AWSSecurityTokenServiceException;
import com.amazonaws.services.securitytoken.model.DecodeAuthorizationMessageRequest;
import com.amazonaws.services.securitytoken.model.DecodeAuthorizationMessageResult;
import com.sequenceiq.cloudbreak.cloud.aws.AwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonSecurityTokenServiceClient;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.common.json.Json;

@Component
public class AwsEncodedAuthorizationFailureMessageDecoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsEncodedAuthorizationFailureMessageDecoder.class);

    private static final Pattern ENCODED_AUTHORIZATION_FAILURE_MESSAGE_PATTERN = Pattern.compile("Encoded authorization failure message: ([^\\s]+)");

    private static final String AWS_ERROR_MESSAGE = "You are not authorized to perform this operation. ";

    private static final String DEFAULT_AUTHORIZATION_ERROR_MESSAGE = "Your credential is not authorized to perform the requested action on AWS side.";

    @Inject
    private AwsClient awsClient;

    public String decodeAuthorizationFailureMessageIfNeeded(AwsCredentialView credentialView, String region, String message) {
        Matcher matcher = ENCODED_AUTHORIZATION_FAILURE_MESSAGE_PATTERN.matcher(message);

        String result = message;
        if (matcher.find()) {
            try {
                result = getResultMessage(credentialView, region, matcher.group(1));
            } catch (AWSSecurityTokenServiceException e) {
                if ("AccessDenied".equals(e.getErrorCode())) {
                    result = replaceAwsMessage(message, matcher.group(0)) + " Please contact your system administrator to update your AWS policy with the " +
                            "sts:DecodeAuthorizationMessage permission to get more details next time.";
                } else {
                    LOGGER.error("Failed to decode authorization failure message", e);
                    result = replaceAwsMessage(message, matcher.group(0));
                }
            } catch (Exception e) {
                LOGGER.error("Failed to decode authorization failure message", e);
                result = replaceAwsMessage(message, matcher.group(0));
            }
        } else {
            LOGGER.debug("Message is not an authorization error, no modification needed: {}", result);
        }

        return result;
    }

    private String getResultMessage(AwsCredentialView credentialView, String region, String encodedMessage) {
        AmazonSecurityTokenServiceClient awsSts = awsClient.createSecurityTokenService(credentialView, region);
        DecodeAuthorizationMessageRequest decodeAuthorizationMessageRequest = new DecodeAuthorizationMessageRequest().withEncodedMessage(encodedMessage);
        DecodeAuthorizationMessageResult decodeAuthorizationMessageResult = awsSts.decodeAuthorizationMessage(decodeAuthorizationMessageRequest);
        String decodedMessage = decodeAuthorizationMessageResult.getDecodedMessage();

        Json authorizationError = new Json(decodedMessage);
        String action = authorizationError.getValue("context.action");
        String resource = authorizationError.getValue("context.resource");

        return String.format("Your AWS credential is not authorized to perform action %s on resource %s. " +
                "Please contact your system administrator to update your AWS policy.", action, resource);
    }

    private String replaceAwsMessage(String message, String match) {
        String result = message.replace(match, "");
        result = result.replace(AWS_ERROR_MESSAGE, "");
        result += DEFAULT_AUTHORIZATION_ERROR_MESSAGE;
        return result;
    }

}
