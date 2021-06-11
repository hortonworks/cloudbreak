package com.sequenceiq.cloudbreak.cloud.aws.resource;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import com.amazonaws.services.ec2.model.AttachVolumeRequest;
import com.amazonaws.services.ec2.model.DescribeVolumesRequest;
import com.amazonaws.services.ec2.model.DescribeVolumesResult;
import com.sequenceiq.cloudbreak.cloud.aws.LegacyAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsPlatformParameters.AwsDiskType;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cloud.template.compute.PreserveResourceException;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@Component
public class AwsAttachmentResourceBuilder extends AbstractAwsComputeBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsAttachmentResourceBuilder.class);

    @Inject
    @Qualifier("intermediateBuilderExecutor")
    private AsyncTaskExecutor intermediateBuilderExecutor;

    @Inject
    private LegacyAwsClient awsClient;

    @Inject
    private VolumeResourceCollector volumeResourceCollector;

    @Override
    public List<CloudResource> create(AwsContext context, long privateId, AuthenticatedContext auth, Group group, Image image) {
        LOGGER.debug("Prepare instance resource to attach to");
        return context.getComputeResources(privateId);
    }

    @Override
    public List<CloudResource> build(AwsContext context, long privateId, AuthenticatedContext auth, Group group,
            List<CloudResource> buildableResource, CloudStack cloudStack) throws Exception {
        LOGGER.debug("Attach volumes to instance");

        CloudResource instance = buildableResource.stream()
                .filter(cloudResource -> cloudResource.getType().equals(ResourceType.AWS_INSTANCE))
                .findFirst()
                .orElseThrow(() -> new AwsResourceException("Instance resource not found"));

        Optional<CloudResource> volumeSetOpt = buildableResource.stream()
                .filter(cloudResource -> cloudResource.getType().equals(ResourceType.AWS_VOLUMESET))
                .findFirst();

        if (volumeSetOpt.isEmpty()) {
            LOGGER.debug("No volumes to attach");
            return List.of();
        }
        CloudResource volumeSet = volumeSetOpt.get();

        AmazonEc2Client client = getAmazonEc2Client(auth);

        VolumeSetAttributes volumeSetAttributes = volumeSet.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class);
        LOGGER.debug("Creating attach volume requests and submitting to executor for stack '{}',   group '{}'",
                auth.getCloudContext().getName(), group.getName());
        List<Future<?>> futures = volumeSetAttributes.getVolumes().stream()
                .filter(volume -> !StringUtils.equals(AwsDiskType.Ephemeral.value(), volume.getType()))
                .map(volume -> new AttachVolumeRequest()
                        .withInstanceId(instance.getInstanceId())
                        .withVolumeId(volume.getId())
                        .withDevice(volume.getDevice()))
                .map(request -> intermediateBuilderExecutor.submit(() -> client.attachVolume(request)))
                .collect(Collectors.toList());

        LOGGER.debug("Waiting for attach volumes request");
        for (Future<?> future : futures) {
            future.get();
        }
        LOGGER.debug("Attach volume requests sent");

        volumeSet.setInstanceId(instance.getInstanceId());
        volumeSet.setStatus(CommonStatus.CREATED);
        return List.of(volumeSet);
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
        AmazonEc2Client client = getAmazonEc2Client(auth);

        Pair<List<String>, List<CloudResource>> volumes = volumeResourceCollector.getVolumeIdsByVolumeResources(resources, resourceType(),
                volumeSetAttributes());

        if (volumes.getFirst().isEmpty()) {
            return collectCloudResourceStatuses(volumes.getSecond(), ResourceStatus.CREATED);
        }

        DescribeVolumesRequest describeVolumesRequest = new DescribeVolumesRequest(volumes.getFirst());
        DescribeVolumesResult result = client.describeVolumes(describeVolumesRequest);
        ResourceStatus volumeSetStatus = result.getVolumes().stream()
                .map(com.amazonaws.services.ec2.model.Volume::getState)
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

    private AmazonEc2Client getAmazonEc2Client(AuthenticatedContext auth) {
        AwsCredentialView credentialView = new AwsCredentialView(auth.getCloudCredential());
        String regionName = auth.getCloudContext().getLocation().getRegion().value();
        return awsClient.createEc2Client(credentialView, regionName);
    }

    @Override
    public int order() {
        return 2;
    }
}
