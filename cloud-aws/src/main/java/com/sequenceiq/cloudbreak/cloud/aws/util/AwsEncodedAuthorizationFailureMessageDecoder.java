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
                    result = message.replaceAll(matcher.group(0), "(Please add sts:DecodeAuthorizationMessage right to your IAM policy to get more details.)");
                } else {
                    LOGGER.error("Failed to decode authorization failure message", e);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to decode authorization failure message", e);
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

        return String.format("You are not authorized to perform action %s on resource %s", action, resource);
    }
}
