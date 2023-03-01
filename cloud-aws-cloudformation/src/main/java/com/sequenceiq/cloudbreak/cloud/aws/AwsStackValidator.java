package com.sequenceiq.cloudbreak.cloud.aws;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.Validator;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;

@Component
public class AwsStackValidator implements Validator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsStackValidator.class);

    @Inject
    private AwsCloudFormationClient awsClient;

    @Inject
    private CloudFormationStackUtil cfStackUtil;

    @Override
    public void validate(AuthenticatedContext ac, CloudStack cloudStack) {
        validateStackNameAvailability(ac);
    }

    private void validateStackNameAvailability(AuthenticatedContext ac) {
        AwsCredentialView credentialView = new AwsCredentialView(ac.getCloudCredential());
        String regionName = ac.getCloudContext().getLocation().getRegion().value();
        AmazonCloudFormationClient cfClient = awsClient.createCloudFormationClient(credentialView, regionName);
        String cFStackName = cfStackUtil.getCfStackName(ac);
        try {
            LOGGER.debug("Checking stack name availability. [{}]", cFStackName);
            cfClient.describeStacks(DescribeStacksRequest.builder().stackName(cFStackName).build());
            throw new CloudConnectorException(String.format("Stack already exists with the given name: %s", cFStackName));
        } catch (AwsServiceException e) {
            if (e.awsErrorDetails().errorMessage().contains(cFStackName + " does not exist")) {
                LOGGER.info("Stack name is available, CF stack not found by name {}", cFStackName);
            } else {
                LOGGER.warn("Exception while checking stack name availability.", e);
            }
        }
    }
}