package com.sequenceiq.freeipa.flow.stack.migration.handler;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.amazonaws.services.cloudformation.model.DescribeStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesResult;
import com.amazonaws.services.cloudformation.model.StackResource;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.freeipa.entity.Stack;

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
        DescribeStackResourcesResult describeStackResourcesResult = awsClient.createCloudFormationClient(awsCredential, regionName)
                .describeStackResources(new DescribeStackResourcesRequest().withStackName(cloudResource.getName()));
        List<StackResource> asGroups = describeStackResourcesResult.getStackResources().stream()
                .filter(stackResource -> "AWS::AutoScaling::AutoScalingGroup".equals(stackResource.getResourceType()))
                .collect(Collectors.toList());
        LOGGER.debug("AutoScalingGroup fetched: {}", asGroups);
        boolean empty = asGroups.stream().map(stackResource -> fetchResult(stackResource, awsCredential, regionName)).allMatch(List::isEmpty);
        LOGGER.debug("Is the fetched AutoScalingGroup are empty? {}", empty);
        return empty;
    }

    private List<String> fetchResult(StackResource autoscalingGroupStackResource, AwsCredentialView awsCredential, String regionName) {
        String physicalResourceId = autoscalingGroupStackResource.getPhysicalResourceId();
        List<String> result = cfStackUtil.getInstanceIds(awsClient.createAutoScalingClient(awsCredential, regionName), physicalResourceId);
        LOGGER.debug("{} autoScalingGroup has {} instance(s): {}", physicalResourceId, result.size(), result);
        return result;
    }

    public String calculateUpgradeVariant(Stack stack, String accountId) {
        String variant = stack.getPlatformvariant();
        boolean migrationEnable = entitlementService.awsVariantMigrationEnable(accountId);
        if (migrationEnable) {
            if (AwsConstants.AwsVariant.AWS_VARIANT.variant().value().equals(variant)) {
                variant = AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.variant().value();
            }
        }
        return variant;
    }

    public boolean isAwsVariantMigrationIsFeasible(Stack stack, String triggeredVariant) {
        Crn crn = Crn.safeFromString(stack.getResourceCrn());
        return AwsConstants.AwsVariant.AWS_VARIANT.variant().value().equals(stack.getCloudPlatform())
                && AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.variant().value().equals(triggeredVariant)
                && entitlementService.awsVariantMigrationEnable(crn.getAccountId());
    }
}
