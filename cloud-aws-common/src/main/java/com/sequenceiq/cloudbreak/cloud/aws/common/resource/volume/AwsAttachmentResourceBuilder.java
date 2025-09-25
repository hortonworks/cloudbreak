package com.sequenceiq.cloudbreak.cloud.aws.common.resource.volume;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cloud.template.compute.PreserveResourceException;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.common.model.AwsDiskType;

import software.amazon.awssdk.services.ec2.model.AttachVolumeRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.Volume;

@Component
public class AwsAttachmentResourceBuilder extends AbstractAwsComputeBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsAttachmentResourceBuilder.class);

    private static final int ORDER = 4;

    @Inject
    @Qualifier("intermediateBuilderExecutor")
    private AsyncTaskExecutor intermediateBuilderExecutor;

    @Inject
    private VolumeResourceCollector volumeResourceCollector;

    @Inject
    private AwsInstanceFinder awsInstanceFinder;

    @Inject
    private CommonAwsClient commonAwsClient;

    @Override
    public List<CloudResource> create(AwsContext context, CloudInstance instance, long privateId, AuthenticatedContext auth, Group group, Image image) {
        LOGGER.debug("Prepare instance resource to attach to");
        return context.getComputeResources(privateId).stream()
                .filter(cloudResource -> ResourceType.AWS_VOLUMESET.equals(cloudResource.getType())).collect(Collectors.toList());
    }

    @Override
    public List<CloudResource> build(AwsContext context, CloudInstance cloudInstance, long privateId, AuthenticatedContext auth, Group group,
            List<CloudResource> buildableResource, CloudStack cloudStack) throws Exception {
        Optional<CloudResource> volumeSetOpt = buildableResource.stream()
                .filter(cloudResource -> cloudResource.getType().equals(ResourceType.AWS_VOLUMESET))
                .findFirst();

        if (volumeSetOpt.isEmpty()) {
            LOGGER.debug("No volumes to attach");
            return List.of();
        }
        CloudResource volumeSet = volumeSetOpt.get();
        String instanceId = awsInstanceFinder.getInstanceId(privateId, context.getComputeResources(privateId));

        AmazonEc2Client client = commonAwsClient.createEc2Client(auth);

        VolumeSetAttributes volumeSetAttributes = volumeSet.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class);
        LOGGER.debug("Attach volume set {} to instance {}", volumeSetAttributes, instanceId);
        LOGGER.debug("Creating attach volume requests and submitting to executor for stack '{}', group '{}'",
                auth.getCloudContext().getName(), group.getName());
        List<Future<?>> futures = volumeSetAttributes.getVolumes().stream()
                .filter(volume -> !StringUtils.equals(AwsDiskType.Ephemeral.value(), volume.getType()))
                .map(volume -> AttachVolumeRequest.builder()
                        .instanceId(instanceId)
                        .volumeId(volume.getId())
                        .device(volume.getDevice())
                        .build())
                .map(request -> intermediateBuilderExecutor.submit(() -> client.attachVolume(request)))
                .collect(Collectors.toList());

        LOGGER.debug("Waiting for attach volumes request");
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Throwable throwable) {
                LOGGER.info("Attachment failed with error.", throwable);
                checkIfEveryVolumesAttachedSuccessfullyEvenIfSomethingFailed(instanceId, client, volumeSetAttributes, throwable);
            }
        }
        LOGGER.debug("Attach volume requests sent");

        volumeSet.setInstanceId(instanceId);
        volumeSet.setStatus(CommonStatus.CREATED);
        return List.of(volumeSet);
    }

    private void checkIfEveryVolumesAttachedSuccessfullyEvenIfSomethingFailed(String instanceId, AmazonEc2Client client,
            VolumeSetAttributes volumeSetAttributes, Throwable throwable) {
        List<String> volumeIdsToAttach =
                volumeSetAttributes.getVolumes().stream().map(VolumeSetAttributes.Volume::getId).collect(Collectors.toList());
        DescribeVolumesResponse describeVolumesResponse = client.describeVolumes(DescribeVolumesRequest.builder()
                .filters(Filter.builder()
                        .name("attachment.instance-id")
                        .values(List.of(instanceId))
                        .build())
                .build());
        LOGGER.info("Describe volume result for instanceid: {}, {}", instanceId, describeVolumesResponse);
        List<Volume> volumes = describeVolumesResponse.volumes();
        List<String> volumeIdsForInstance = volumes.stream().map(Volume::volumeId).toList();
        LOGGER.info("Volume IDs to attach {}", volumeIdsToAttach);
        if (!new HashSet<>(volumeIdsForInstance).containsAll(volumeIdsToAttach)) {
            String errorMessage = "Volume attachment were unsuccessful. ";
            if (throwable.getMessage().contains("is not 'running'")) {
                errorMessage += "The related instance is not available. Usually this happens when an AWS policy terminates the instance, " +
                        "or because of a quota issue. Please check the AWS console! ";
            }
            LOGGER.error(errorMessage);
            throw new CloudbreakServiceException(errorMessage + throwable.getMessage(), throwable);
        }
    }

    @Override
    public CloudResource delete(AwsContext context, AuthenticatedContext auth, CloudResource resource) throws PreserveResourceException {
        throw new PreserveResourceException("Prevent volume resource deletion.");
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.AWS_VOLUMESET;
    }

    @Override
    protected List<CloudResourceStatus> checkResources(ResourceType type, AwsContext context, AuthenticatedContext auth, Iterable<CloudResource> resources) {
        AmazonEc2Client client = commonAwsClient.createEc2Client(auth);

        Pair<List<String>, List<CloudResource>> volumes = volumeResourceCollector.getVolumeIdsByVolumeResources(resources, resourceType(),
                volumeSetAttributes());

        if (volumes.getFirst().isEmpty()) {
            return collectCloudResourceStatuses(volumes.getSecond(), ResourceStatus.CREATED);
        }

        DescribeVolumesRequest describeVolumesRequest = DescribeVolumesRequest.builder().volumeIds(volumes.getFirst()).build();
        DescribeVolumesResponse result = client.describeVolumes(describeVolumesRequest);
        ResourceStatus volumeSetStatus = result.volumes().stream()
                .map(Volume::stateAsString)
                .allMatch("in-use"::equals) ? ResourceStatus.CREATED : ResourceStatus.IN_PROGRESS;
        LOGGER.debug("[{}] volume set status is {}", String.join(",", volumes.getFirst()), volumeSetStatus);
        return collectCloudResourceStatuses(volumes.getSecond(), volumeSetStatus);
    }

    private List<CloudResourceStatus> collectCloudResourceStatuses(List<CloudResource> volumeResources, ResourceStatus status) {
        return volumeResources.stream()
                .map(resource -> new CloudResourceStatus(resource, status))
                .collect(Collectors.toList());
    }

    private Function<CloudResource, VolumeSetAttributes> volumeSetAttributes() {
        return volumeSet -> volumeSet.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class);
    }

    @Override
    public int order() {
        return ORDER;
    }
}
