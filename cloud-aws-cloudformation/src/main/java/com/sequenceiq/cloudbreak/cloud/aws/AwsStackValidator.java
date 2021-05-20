package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceTypesRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceTypesResult;
import com.amazonaws.services.ec2.model.InstanceTypeInfo;
import com.sequenceiq.cloudbreak.cloud.Validator;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;

@Component
public class AwsStackValidator implements Validator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsStackValidator.class);

    @Inject
    private LegacyAwsClient awsClient;

    @Inject
    private CloudFormationStackUtil cfStackUtil;

    @Override
    public void validate(AuthenticatedContext ac, CloudStack cloudStack) {
        validateStackNameAvailability(ac);
        validateInstanceStorageOnInstanceTypes(ac, cloudStack);
    }

    private void validateStackNameAvailability(AuthenticatedContext ac) {
        AwsCredentialView credentialView = new AwsCredentialView(ac.getCloudCredential());
        String regionName = ac.getCloudContext().getLocation().getRegion().value();
        AmazonCloudFormationClient cfClient = awsClient.createCloudFormationClient(credentialView, regionName);
        String cFStackName = cfStackUtil.getCfStackName(ac);
        try {
            LOGGER.debug("Checking stack name availability. [{}]", cFStackName);
            cfClient.describeStacks(new DescribeStacksRequest().withStackName(cFStackName));
            throw new CloudConnectorException(String.format("Stack is already exists with the given name: %s", cFStackName));
        } catch (AmazonServiceException e) {
            if (e.getMessage().toLowerCase().contains("not found")) {
                LOGGER.info("Stack name is available, CF stack not found by name {}", cFStackName);
            } else {
                LOGGER.warn("Exception while checking stack name availability.", e);
            }
        }
    }

    private void validateInstanceStorageOnInstanceTypes(AuthenticatedContext ac, CloudStack cloudStack) {
        LOGGER.debug("Check instance storage availability on instance types");
        List<String> instanceTypes = cloudStack.getGroups().stream()
                .map(Group::getInstances)
                .flatMap(Collection::stream)
                .filter(toEphemeralStorageTemplates())
                .map(CloudInstance::getTemplate)
                .map(InstanceTemplate::getFlavor)
                .collect(Collectors.toList());
        LOGGER.debug("Instance types to check: {}", instanceTypes);
        List<String> notSupportedTypes = getInstanceStorageNotSupportedTypes(ac, instanceTypes);
        if (!CollectionUtils.isEmpty(notSupportedTypes)) {
            LOGGER.warn("The following instance types does not support instance storage: {}", notSupportedTypes);
            throw new CloudConnectorException(String.format("The following instance types does not support instance storage: %s", notSupportedTypes));
        }
    }

    private Predicate<CloudInstance> toEphemeralStorageTemplates() {
        return instance -> instance.getTemplate().getTemporaryStorage() == TemporaryStorage.EPHEMERAL_VOLUMES;
    }

    private List<String> getInstanceStorageNotSupportedTypes(AuthenticatedContext authenticatedContext, List<String> instanceTypes) {
        AwsCredentialView credentialView = new AwsCredentialView(authenticatedContext.getCloudCredential());
        AmazonEc2Client amazonEC2Client = awsClient.createEc2Client(credentialView);
        DescribeInstanceTypesRequest request = new DescribeInstanceTypesRequest().withInstanceTypes(instanceTypes);
        try {
            DescribeInstanceTypesResult result = amazonEC2Client.describeInstanceTypes(request);
            return result.getInstanceTypes().stream()
                    .filter(types -> !types.getInstanceStorageSupported())
                    .map(InstanceTypeInfo::getInstanceType)
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            LOGGER.warn("Failed to describe instance types: {}", instanceTypes, ex);
            throw ex;
        }
    }
}