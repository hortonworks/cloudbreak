package com.sequenceiq.cloudbreak.cloud.aws.common.kms;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonKmsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.template.compute.PreserveResourceException;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.services.kms.model.ScheduleKeyDeletionRequest;

@Component
public class AwsKmsKeyResourceBuilder extends AbstractAwsComputeBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsKmsKeyResourceBuilder.class);

    @Value("${cb.aws.kms.delete.pendingWindowInDays:7}")
    private Integer pendingWindowInDays;

    @Inject
    private CommonAwsClient awsClient;

    @Override
    public CloudResource delete(AwsContext context, AuthenticatedContext auth, CloudResource resource) throws PreserveResourceException {
        AmazonKmsClient kmsClient = awsClient.createAWSKMS(auth);
        ScheduleKeyDeletionRequest scheduleKeyDeletionRequest = ScheduleKeyDeletionRequest
                .builder()
                .keyId(resource.getReference())
                .pendingWindowInDays(pendingWindowInDays)
                .build();
        kmsClient.scheduleKeyDeletion(scheduleKeyDeletionRequest);
        return resource;
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.AWS_KMS_KEY;
    }

    @Override
    public int order() {
        return LOWEST_PRECEDENCE;
    }
}
