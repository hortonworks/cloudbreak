package com.sequenceiq.cloudbreak.cloud.aws;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.sequenceiq.cloudbreak.cloud.Validator;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

@Component
public class AwsStackValidator implements Validator {

    @Inject
    private AwsClient awsClient;

    @Inject
    private CloudFormationStackUtil cfStackUtil;

    @Override
    public void validate(AuthenticatedContext ac, CloudStack cloudStack) {
        AwsCredentialView credentialView = new AwsCredentialView(ac.getCloudCredential());
        String regionName = ac.getCloudContext().getLocation().getRegion().value();
        AmazonCloudFormationClient cfClient = awsClient.createCloudFormationClient(credentialView, regionName);
        String cFStackName = cfStackUtil.getCfStackName(ac);
        try {
            cfClient.describeStacks(new DescribeStacksRequest().withStackName(cFStackName));
            throw new CloudConnectorException(String.format("Stack is already exists with the given name: %s", cFStackName));
        } catch (AmazonServiceException ignored) {

        }
    }
}