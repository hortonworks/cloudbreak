package com.sequenceiq.cloudbreak.cloud.aws.resource;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;

import com.amazonaws.services.ec2.model.AttachVolumeRequest;
import com.amazonaws.services.ec2.model.DescribeVolumesRequest;
import com.amazonaws.services.ec2.model.DescribeVolumesResult;
import com.sequenceiq.cloudbreak.cloud.aws.AwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.AwsPlatformParameters.AwsDiskType;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonEc2RetryClient;
import com.sequenceiq.cloudbreak.cloud.aws.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@Component
public class AwsAttachmentResourceBuilder extends AbstractAwsComputeBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsAttachmentResourceBuilder.class);

    @Inject
    @Qualifier("intermediateBuilderExecutor")
    private AsyncTaskExecutor intermediateBuilderExecutor;

    @Inject
    private AwsClient awsClient;

    @Inject
    private Retry retry;

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

        AmazonEc2RetryClient client = getAmazonEc2RetryClient(auth);

        VolumeSetAttributes volumeSetAttributes = volumeSet.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class);
        List<Future<?>> futures = volumeSetAttributes.getVolumes().stream()
                .filter(volume -> !StringUtils.equals(AwsDiskType.Ephemeral.value(), volume.getType()))
                .map(volume -> new AttachVolumeRequest()
                        .withInstanceId(instance.getInstanceId())
                        .withVolumeId(volume.getId())
                        .withDevice(volume.getDevice()))
                .map(request -> intermediateBuilderExecutor.submit(() -> client.attachVolume(request)))
                .collect(Collectors.toList());

        for (Future<?> future : futures) {
            future.get();
        }

        volumeSet.setInstanceId(instance.getInstanceId());
        volumeSet.setStatus(CommonStatus.CREATED);
        return List.of(volumeSet);
    }

    @Override
    public CloudResource delete(AwsContext context, AuthenticatedContext auth, CloudResource resource) throws InterruptedException {
        throw new InterruptedException("Prevent volume resource deletion.");
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.AWS_VOLUMESET;
    }

    @Override
    protected List<CloudResourceStatus> checkResources(ResourceType type, AwsContext context, AuthenticatedContext auth, Iterable<CloudResource> resources) {

        AmazonEc2RetryClient client = getAmazonEc2RetryClient(auth);
        List<CloudResource> volumeResources = StreamSupport.stream(resources.spliterator(), false)
                .filter(r -> r.getType().equals(resourceType()))
                .collect(Collectors.toList());
        List<String> volumeIds = volumeResources.stream()
                .map(volumeSetAttributes())
                .map(VolumeSetAttributes::getVolumes)
                .flatMap(List::stream)
                .map(VolumeSetAttributes.Volume::getId)
                .collect(Collectors.toList());

        if (volumeIds.isEmpty()) {
            return collectCloudResourceStatuses(volumeResources, ResourceStatus.CREATED);
        }

        DescribeVolumesRequest describeVolumesRequest = new DescribeVolumesRequest(volumeIds);
        DescribeVolumesResult result = client.describeVolumes(describeVolumesRequest);
        ResourceStatus volumeSetStatus = result.getVolumes().stream()
                .map(com.amazonaws.services.ec2.model.Volume::getState)
                .allMatch("in-use"::equals) ? ResourceStatus.CREATED : ResourceStatus.IN_PROGRESS;
        return collectCloudResourceStatuses(volumeResources, volumeSetStatus);
    }

    private List<CloudResourceStatus> collectCloudResourceStatuses(List<CloudResource> volumeResources, ResourceStatus status) {
        return volumeResources.stream()
                .map(resource -> new CloudResourceStatus(resource, status))
                .collect(Collectors.toList());
    }

    private Function<CloudResource, VolumeSetAttributes> volumeSetAttributes() {
        return volumeSet -> volumeSet.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class);
    }

    private AmazonEc2RetryClient getAmazonEc2RetryClient(AuthenticatedContext auth) {
        AwsCredentialView credentialView = new AwsCredentialView(auth.getCloudCredential());
        String regionName = auth.getCloudContext().getLocation().getRegion().value();
        return new AmazonEc2RetryClient(awsClient.createAccess(credentialView, regionName), retry);
    }

    @Override
    public int order() {
        return 2;
    }
}
