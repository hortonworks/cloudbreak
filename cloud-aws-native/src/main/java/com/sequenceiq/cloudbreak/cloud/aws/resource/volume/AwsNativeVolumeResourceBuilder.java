package com.sequenceiq.cloudbreak.cloud.aws.resource.volume;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants.AwsVariant.AWS_NATIVE_VARIANT;
import static com.sequenceiq.cloudbreak.cloud.aws.resource.AwsNativeResourceBuilderOrderConstants.NATIVE_VOLUME_RESOURCE_BUILDER_ORDER;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.aws.common.resource.volume.AwsVolumeResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.services.ec2.model.ArchitectureValues;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceStatusRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceStatusResponse;
import software.amazon.awssdk.services.ec2.model.SummaryStatus;

@Component
public class AwsNativeVolumeResourceBuilder extends AwsVolumeResourceBuilder {

    private static final Logger LOGGER = getLogger(AwsNativeVolumeResourceBuilder.class);

    @Override
    public Variant variant() {
        return AWS_NATIVE_VARIANT.variant();
    }

    @Override
    protected String getSubnetId(AwsContext context, CloudInstance cloudInstance) {
        return cloudInstance.getSubnetId();
    }

    protected Optional<String> getAvailabilityZone(AwsContext context, CloudInstance cloudInstance) {
        return Optional.ofNullable(cloudInstance.getAvailabilityZone());
    }

    @Override
    public int order() {
        return NATIVE_VOLUME_RESOURCE_BUILDER_ORDER;
    }

    @Override
    protected List<CloudResourceStatus> checkResources(ResourceType type, AwsContext context, AuthenticatedContext auth, Iterable<CloudResource> resources) {
        List<CloudResourceStatus> cloudResourceStatuses = super.checkResources(type, context, auth, resources);

        List<CloudResource> cloudResources = collectCloudResourcesBasedOnType(resources, ResourceType.AWS_VOLUMESET);
        Optional<CloudResource> cloudResource = cloudResources.stream().findFirst();
        if (cloudResource.isEmpty()) {
            LOGGER.warn("Volume Set cloud resource not found, skipping instance system check");
            return cloudResourceStatuses;
        }
        CloudResource volumeSet = cloudResource.get();
        String instanceId = volumeSet.getInstanceId();
        LOGGER.debug("Got instanceId for system status check: {}", instanceId);
        String architecture = volumeSet.getParameter(CloudResource.ARCHITECTURE, String.class);
        LOGGER.debug("Got architecture for system status check: {}", architecture);

        if (ArchitectureValues.ARM64.name().equals(architecture)) {
            return mapResourceStatusBasedOnInstanceStatus(context, instanceId, cloudResourceStatuses);
        } else {
            LOGGER.debug("Instance architecture is not ARM64: {}, skipping instance system check", architecture);
            return cloudResourceStatuses;
        }
    }

    private List<CloudResource> collectCloudResourcesBasedOnType(Iterable<CloudResource> resources, ResourceType resourceType) {
        List<CloudResource> cloudResources = stream(resources.spliterator(), false)
                .filter(r -> r.getType().equals(resourceType))
                .collect(toList());
        LOGGER.debug("The following cloud resource(s) has been collected based on the requested type ({}): [{}]",
                resourceType != null ? resourceType.name() : "null",
                cloudResources.stream().map(CloudResource::toString).collect(Collectors.joining(",")));
        return cloudResources;
    }

    private List<CloudResourceStatus> mapResourceStatusBasedOnInstanceStatus(AwsContext context, String instanceId,
            List<CloudResourceStatus> cloudResourceStatuses) {
        DescribeInstanceStatusResponse describeInstanceStatusResponse = context.getAmazonEc2Client().describeInstanceStatus(
                DescribeInstanceStatusRequest.builder()
                        .instanceIds(instanceId)
                        .build());
        Optional<Boolean> finishedStatusChecks = describeInstanceStatusResponse.instanceStatuses().stream()
                .peek(instanceStatus -> LOGGER.debug("Instance status checks: {}", instanceStatus))
                .map(instanceStatus -> !SummaryStatus.INITIALIZING.equals(instanceStatus.systemStatus().status())).findFirst();
        if (finishedStatusChecks.isPresent() && finishedStatusChecks.get()) {
            LOGGER.debug("Instance system status check reached a final state");
            return cloudResourceStatuses;
        } else {
            LOGGER.debug("Instance system status checks is not ok, setting resource status to IN_PROGRESS");
            return cloudResourceStatuses.stream()
                    .map(resource -> new CloudResourceStatus(resource.getCloudResource(), ResourceStatus.IN_PROGRESS))
                    .collect(Collectors.toList());
        }
    }
}
