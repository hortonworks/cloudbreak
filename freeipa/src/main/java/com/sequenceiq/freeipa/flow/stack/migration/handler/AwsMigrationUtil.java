package com.sequenceiq.freeipa.flow.stack.migration.handler;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.freeipa.entity.Stack;

import software.amazon.awssdk.services.cloudformation.model.DescribeStackResourcesRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackResourcesResponse;
import software.amazon.awssdk.services.cloudformation.model.StackResource;

@Component
public class AwsMigrationUtil {

    private static final Logger LOGGER = getLogger(AwsMigrationUtil.class);

    @Inject
    private CloudFormationStackUtil cfStackUtil;

    @Inject
    private AwsCloudFormationClient awsClient;

    @Inject
    private EntitlementService entitlementService;

    public boolean allInstancesDeletedFromCloudFormation(AuthenticatedContext ac, CloudResource cloudResource) {
        String regionName = ac.getCloudContext().getLocation().getRegion().value();
        AwsCredentialView awsCredential = new AwsCredentialView(ac.getCloudCredential());
        DescribeStackResourcesResponse describeStackResourcesResponse = awsClient.createCloudFormationClient(awsCredential, regionName)
                .describeStackResources(DescribeStackResourcesRequest.builder().stackName(cloudResource.getName()).build());
        List<StackResource> asGroups = describeStackResourcesResponse.stackResources().stream()
                .filter(stackResource -> "AWS::AutoScaling::AutoScalingGroup".equals(stackResource.resourceType()))
                .collect(Collectors.toList());
        LOGGER.debug("AutoScalingGroup fetched: {}", asGroups);
        boolean empty = asGroups.stream().map(stackResource -> fetchResult(stackResource, awsCredential, regionName)).allMatch(List::isEmpty);
        LOGGER.debug("Is the fetched AutoScalingGroup empty? {}", empty);
        return empty;
    }

    private List<String> fetchResult(StackResource autoscalingGroupStackResource, AwsCredentialView awsCredential, String regionName) {
        String physicalResourceId = autoscalingGroupStackResource.physicalResourceId();
        List<String> result = cfStackUtil.getInstanceIds(awsClient.createAutoScalingClient(awsCredential, regionName), physicalResourceId);
        LOGGER.debug("{} autoScalingGroup has {} instance(s): {}", physicalResourceId, result.size(), result);
        return result;
    }

    public String calculateUpgradeVariant(Stack stack, String accountId) {
        String variant = stack.getPlatformvariant();
        boolean migrationEnabled = entitlementService.awsVariantMigrationEnabled(accountId);
        if (migrationEnabled) {
            if (AwsConstants.AwsVariant.AWS_VARIANT.variant().value().equals(variant)) {
                variant = AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.variant().value();
            }
        }
        return variant;
    }

    public boolean awsVariantMigrationIsFeasible(Stack stack, String triggeredVariant) {
        Crn crn = Crn.safeFromString(stack.getResourceCrn());
        return AwsConstants.AwsVariant.AWS_VARIANT.variant().value().equals(stack.getCloudPlatform())
                && AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.variant().value().equals(triggeredVariant)
                && entitlementService.awsVariantMigrationEnabled(crn.getAccountId());
    }
}
