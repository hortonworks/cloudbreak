package com.sequenceiq.cloudbreak.cloud.aws.resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ec2.model.CreateVolumeRequest;
import com.amazonaws.services.ec2.model.CreateVolumeResult;
import com.amazonaws.services.ec2.model.DeleteVolumeRequest;
import com.amazonaws.services.ec2.model.DescribeSubnetsRequest;
import com.amazonaws.services.ec2.model.DescribeSubnetsResult;
import com.amazonaws.services.ec2.model.DescribeVolumesRequest;
import com.amazonaws.services.ec2.model.DescribeVolumesResult;
import com.amazonaws.services.ec2.model.EbsInstanceBlockDeviceSpecification;
import com.amazonaws.services.ec2.model.InstanceBlockDeviceMappingSpecification;
import com.amazonaws.services.ec2.model.ModifyInstanceAttributeRequest;
import com.amazonaws.services.ec2.model.ModifyInstanceAttributeResult;
import com.amazonaws.services.ec2.model.Subnet;
import com.amazonaws.services.ec2.model.TagSpecification;
import com.sequenceiq.cloudbreak.cloud.aws.AwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.AwsPlatformParameters.AwsDiskType;
import com.sequenceiq.cloudbreak.cloud.aws.AwsTagPreparationService;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonEc2RetryClient;
import com.sequenceiq.cloudbreak.cloud.aws.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.aws.encryption.EncryptedSnapshotService;
import com.sequenceiq.cloudbreak.cloud.aws.service.AwsResourceNameService;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource.Builder;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes.Volume;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.cloudbreak.util.DeviceNameGenerator;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@Component
public class AwsVolumeResourceBuilder extends AbstractAwsComputeBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsVolumeResourceBuilder.class);

    private static final String DEVICE_NAME_TEMPLATE = "/dev/xvd%s";

    @Inject
    @Qualifier("intermediateBuilderExecutor")
    private AsyncTaskExecutor intermediateBuilderExecutor;

    @Inject
    private PersistenceNotifier resourceNotifier;

    @Inject
    private AwsTagPreparationService awsTagPreparationService;

    @Inject
    private EncryptedSnapshotService encryptedSnapshotService;

    @Inject
    private AwsClient awsClient;

    @Inject
    private Retry retry;

    private Function<Volume, InstanceBlockDeviceMappingSpecification> toInstanceBlockDeviceMappingSpecification = volume -> {
        EbsInstanceBlockDeviceSpecification device = new EbsInstanceBlockDeviceSpecification()
                .withVolumeId(volume.getId())
                .withDeleteOnTermination(Boolean.TRUE);

        return new InstanceBlockDeviceMappingSpecification()
                .withEbs(device)
                .withDeviceName(volume.getDevice());
    };

    @Override
    public List<CloudResource> create(AwsContext context, long privateId, AuthenticatedContext auth, Group group, Image image) {
        LOGGER.debug("Create volume resources");

        CloudInstance instance = group.getReferenceInstanceConfiguration();
        InstanceTemplate template = instance.getTemplate();
        if (CollectionUtils.isEmpty(template.getVolumes())) {
            LOGGER.debug("No volume requested");
            return List.of();
        }

        List<CloudResource> computeResources = context.getComputeResources(privateId);
        Optional<CloudResource> reattachableVolumeSet = computeResources.stream()
                .filter(resource -> ResourceType.AWS_VOLUMESET.equals(resource.getType()))
                .findFirst();

        CloudResource subnet = context.getNetworkResources().stream()
                .filter(cloudResource -> ResourceType.AWS_SUBNET.equals(cloudResource.getType())).findFirst().get();
        return List.of(reattachableVolumeSet.orElseGet(createVolumeSet(privateId, auth, group, subnet)));
    }

    private Supplier<CloudResource> createVolumeSet(long privateId, AuthenticatedContext auth, Group group, CloudResource subnetResource) {
        return () -> {
            AwsResourceNameService resourceNameService = getResourceNameService();

            CloudInstance instance = group.getReferenceInstanceConfiguration();
            InstanceTemplate template = instance.getTemplate();
            String groupName = group.getName();
            CloudContext cloudContext = auth.getCloudContext();
            String stackName = cloudContext.getName();

            String availabilityZone = getAvailabilityZoneFromSubnet(auth, subnetResource);

            return new Builder()
                    .persistent(true)
                    .type(resourceType())
                    .name(resourceNameService.resourceName(resourceType(), stackName, groupName, privateId))
                    .group(group.getName())
                    .status(CommonStatus.REQUESTED)
                    .params(Map.of(CloudResource.ATTRIBUTES, new VolumeSetAttributes.Builder()
                            .withAvailabilityZone(availabilityZone)
                            .withDeleteOnTermination(Boolean.TRUE)
                            .withVolumes(template.getVolumes().stream()
                                    .filter(vol -> !AwsDiskType.Ephemeral.value().equalsIgnoreCase(vol.getType()))
                                    .map(vol -> new Volume(null, null, vol.getSize(), vol.getType()))
                                    .collect(Collectors.toList()))
                            .build()))
                    .build();
        };
    }

    private String getAvailabilityZoneFromSubnet(AuthenticatedContext auth, CloudResource subnetResource) {
        AmazonEc2RetryClient amazonEC2Client = getAmazonEC2Client(auth);
        DescribeSubnetsResult describeSubnetsResult = amazonEC2Client.describeSubnets(new DescribeSubnetsRequest()
                .withSubnetIds(subnetResource.getName()));
        return describeSubnetsResult.getSubnets().stream()
                .filter(subnet -> subnetResource.getName().equals(subnet.getSubnetId()))
                .map(Subnet::getAvailabilityZone)
                .findFirst()
                .orElse(auth.getCloudContext().getLocation().getAvailabilityZone().value());
    }

    @Override
    public List<CloudResource> build(AwsContext context, long privateId, AuthenticatedContext auth, Group group,
            List<CloudResource> buildableResource, CloudStack cloudStack) throws Exception {
        LOGGER.debug("Create volumes on provider");
        AmazonEc2RetryClient client = getAmazonEC2Client(auth);

        Map<String, List<Volume>> volumeSetMap = Collections.synchronizedMap(new HashMap<>());

        List<Future<?>> futures = new ArrayList<>();
        String snapshotId = getEbsSnapshotIdIfNeeded(auth, cloudStack, group);
        TagSpecification tagSpecification = new TagSpecification()
                .withResourceType(com.amazonaws.services.ec2.model.ResourceType.Volume)
                .withTags(awsTagPreparationService.prepareEc2Tags(auth, cloudStack.getTags()));

        List<CloudResource> requestedResources = buildableResource.stream()
                .filter(cloudResource -> CommonStatus.REQUESTED.equals(cloudResource.getStatus()))
                .collect(Collectors.toList());
        Long ephemeralCount = group.getReferenceInstanceConfiguration().getTemplate().getVolumes().stream()
                .filter(vol -> AwsDiskType.Ephemeral.value().equalsIgnoreCase(vol.getType())).collect(Collectors.counting());

        for (CloudResource resource : requestedResources) {
            volumeSetMap.put(resource.getName(), Collections.synchronizedList(new ArrayList<>()));

            VolumeSetAttributes volumeSet = resource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class);
            DeviceNameGenerator generator = new DeviceNameGenerator(DEVICE_NAME_TEMPLATE, ephemeralCount.intValue());
            futures.addAll(volumeSet.getVolumes().stream()
                    .map(createVolumeRequest(snapshotId, tagSpecification, volumeSet))
                    .map(request -> intermediateBuilderExecutor.submit(() -> {
                        CreateVolumeResult result = client.createVolume(request);
                        String volumeId = result.getVolume().getVolumeId();
                        Volume volume = new Volume(volumeId, generator.next(), request.getSize(), request.getVolumeType());
                        volumeSetMap.get(resource.getName()).add(volume);
                    }))
                    .collect(Collectors.toList()));
        }

        for (Future<?> future : futures) {
            future.get();
        }

        return buildableResource.stream()
                .peek(resource -> {
                    List<Volume> volumes = volumeSetMap.get(resource.getName());
                    if (!CollectionUtils.isEmpty(volumes)) {
                        resource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class).setVolumes(volumes);
                    }
                })
                .map(copyResourceWithNewStatus(CommonStatus.CREATED))
                .collect(Collectors.toList());
    }

    private Function<CloudResource, CloudResource> copyResourceWithNewStatus(CommonStatus status) {
        return resource -> new Builder()
                .persistent(true)
                .group(resource.getGroup())
                .type(resource.getType())
                .status(status)
                .name(resource.getName())
                .params(resource.getParameters())
                .build();
    }

    private Function<VolumeSetAttributes.Volume, CreateVolumeRequest> createVolumeRequest(String snapshotId, TagSpecification tagSpecification,
            VolumeSetAttributes volumeSet) {
        return volume -> new CreateVolumeRequest()
                .withAvailabilityZone(volumeSet.getAvailabilityZone())
                .withSize(volume.getSize())
                .withSnapshotId(snapshotId)
                .withTagSpecifications(tagSpecification)
                .withVolumeType(volume.getType());
    }

    private String getEbsSnapshotIdIfNeeded(AuthenticatedContext ac, CloudStack cloudStack, Group group) {
        if (!encryptedSnapshotService.isEncryptedVolumeRequested(group)) {
            return null;
        }

        return encryptedSnapshotService.createSnapshotIfNeeded(ac, cloudStack, group, resourceNotifier)
                .orElseThrow(() -> {
                    String message = String.format("Failed to create Ebs encrypted volume on stack: %s", ac.getCloudContext().getId());
                    return new CloudConnectorException(message);
                });
    }

    @Override
    public CloudResource delete(AwsContext context, AuthenticatedContext auth, CloudResource resource) throws InterruptedException {
        LOGGER.debug("Set delete on termination to true, on instances");
        VolumeSetAttributes volumeSetAttributes = resource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class);
        List<CloudResourceStatus> cloudResourceStatuses = checkResources(ResourceType.AWS_VOLUMESET, context, auth, List.of(resource));

        boolean anyDeleted = cloudResourceStatuses.stream().map(CloudResourceStatus::getStatus).anyMatch(ResourceStatus.DELETED::equals);
        if (!volumeSetAttributes.getDeleteOnTermination() && !anyDeleted) {
            LOGGER.debug("Volumes will be preserved.");
            volumeSetAttributes.setDeleteOnTermination(Boolean.TRUE);
            resource.putParameter(CloudResource.ATTRIBUTES, volumeSetAttributes);
            resourceNotifier.notifyUpdate(resource, auth.getCloudContext());
            throw new InterruptedException("Resource will be preserved for later reattachment.");
        }

        AmazonEc2RetryClient client = getAmazonEC2Client(auth);
        deleteOrphanedVolumes(cloudResourceStatuses, client);
        turnOnDeleteOnterminationOnAttachedVolumes(resource, cloudResourceStatuses, client);

        return null;
    }

    private void turnOnDeleteOnterminationOnAttachedVolumes(CloudResource resource, List<CloudResourceStatus> cloudResourceStatuses,
            AmazonEc2RetryClient client) {
        List<InstanceBlockDeviceMappingSpecification> deviceMappingSpecifications = cloudResourceStatuses.stream()
                .filter(cloudResourceStatus -> ResourceStatus.ATTACHED.equals(cloudResourceStatus.getStatus()))
                .map(CloudResourceStatus::getCloudResource)
                .map(cloudResource -> cloudResource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class))
                .map(VolumeSetAttributes::getVolumes)
                .flatMap(List::stream)
                .map(toInstanceBlockDeviceMappingSpecification)
                .collect(Collectors.toList());
        ModifyInstanceAttributeRequest modifyInstanceAttributeRequest = new ModifyInstanceAttributeRequest()
                .withInstanceId(resource.getInstanceId())
                .withBlockDeviceMappings(deviceMappingSpecifications);

        ModifyInstanceAttributeResult modifyIdentityIdFormatResult = client.modifyInstanceAttribute(modifyInstanceAttributeRequest);
        LOGGER.debug("Delete on termination set to ture. {}", modifyIdentityIdFormatResult);
    }

    private void deleteOrphanedVolumes(List<CloudResourceStatus> cloudResourceStatuses, AmazonEc2RetryClient client) {
        cloudResourceStatuses.stream()
                .filter(cloudResourceStatus -> ResourceStatus.CREATED.equals(cloudResourceStatus.getStatus()))
                .map(CloudResourceStatus::getCloudResource)
                .map(cloudResource -> cloudResource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class))
                .map(VolumeSetAttributes::getVolumes)
                .flatMap(List::stream)
                .map(VolumeSetAttributes.Volume::getId)
                .map(volumeId -> new DeleteVolumeRequest().withVolumeId(volumeId))
                .forEach(client::deleteVolume);
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.AWS_VOLUMESET;
    }

    @Override
    protected List<CloudResourceStatus> checkResources(ResourceType type, AwsContext context, AuthenticatedContext auth, Iterable<CloudResource> resources) {
        AmazonEc2RetryClient client = getAmazonEC2Client(auth);
        List<CloudResource> volumeResources = StreamSupport.stream(resources.spliterator(), false)
                .filter(r -> r.getType().equals(resourceType()))
                .collect(Collectors.toList());
        List<String> volumeIds = volumeResources.stream()
                .map(volumeSetAttributes())
                .map(VolumeSetAttributes::getVolumes)
                .flatMap(List::stream)
                .map(VolumeSetAttributes.Volume::getId)
                .collect(Collectors.toList());

        DescribeVolumesRequest describeVolumesRequest = new DescribeVolumesRequest(volumeIds);
        DescribeVolumesResult result = client.describeVolumes(describeVolumesRequest);
        ResourceStatus volumeSetStatus = getResourceStatus(result);
        LOGGER.debug("Reduced resource status for volume set is {}", volumeSetStatus);
        return volumeResources.stream()
                .map(resource -> new CloudResourceStatus(resource, volumeSetStatus))
                .collect(Collectors.toList());
    }

    private ResourceStatus getResourceStatus(DescribeVolumesResult result) {
        try {
            return result.getVolumes().stream()
                    .peek(volume -> LOGGER.debug("State of volume {} is {}", volume.getVolumeId(), volume.getState()))
                    .map(com.amazonaws.services.ec2.model.Volume::getState)
                    .map(toResourceStatus())
                    .reduce(ResourceStatus.ATTACHED, resourceStatusReducer());
        } catch (AmazonEC2Exception e) {
            if ("InvalidVolume.NotFound".equals(e.getErrorCode())) {
                return ResourceStatus.DELETED;
            }
            return ResourceStatus.FAILED;
        }
    }

    private BinaryOperator<ResourceStatus> resourceStatusReducer() {
        return (statusA, statusB) -> {
            List<ResourceStatus> statuses = List.of(statusA, statusB);
            if (statuses.contains(ResourceStatus.DELETED)) {
                return ResourceStatus.DELETED;
            } else if (statuses.contains(ResourceStatus.IN_PROGRESS)) {
                return ResourceStatus.IN_PROGRESS;
            } else if (statuses.contains(ResourceStatus.CREATED)) {
                return ResourceStatus.CREATED;
            }

            return ResourceStatus.ATTACHED;
        };
    }

    private Function<String, ResourceStatus> toResourceStatus() {
        return state -> {
            switch (state) {
                case "available":
                    return ResourceStatus.CREATED;
                case "in-use":
                    return ResourceStatus.ATTACHED;
                case "deleting":
                    return ResourceStatus.DELETED;
                case "creating":
                default:
                    return ResourceStatus.IN_PROGRESS;
            }
        };
    }

    private Function<CloudResource, VolumeSetAttributes> volumeSetAttributes() {
        return volumeSet -> volumeSet.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class);
    }

    private AmazonEc2RetryClient getAmazonEC2Client(AuthenticatedContext auth) {
        AwsCredentialView credentialView = new AwsCredentialView(auth.getCloudCredential());
        String regionName = auth.getCloudContext().getLocation().getRegion().value();
        return new AmazonEc2RetryClient(awsClient.createAccess(credentialView, regionName), retry);

    }

    @Override
    public int order() {
        return 1;
    }
}
