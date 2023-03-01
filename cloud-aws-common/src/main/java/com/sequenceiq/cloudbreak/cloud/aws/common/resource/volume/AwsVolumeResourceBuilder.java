package com.sequenceiq.cloudbreak.cloud.aws.common.resource.volume;

import static com.sequenceiq.cloudbreak.cloud.model.ResourceStatus.ATTACHED;
import static com.sequenceiq.cloudbreak.cloud.model.ResourceStatus.CREATED;
import static com.sequenceiq.cloudbreak.cloud.model.ResourceStatus.DELETED;
import static com.sequenceiq.cloudbreak.cloud.model.ResourceStatus.FAILED;
import static com.sequenceiq.cloudbreak.cloud.model.ResourceStatus.IN_PROGRESS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.cloud.aws.common.AwsTaggingService;
import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.aws.common.service.AwsResourceNameService;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsMethodExecutor;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsInstanceView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes.Volume;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.service.ResourceRetriever;
import com.sequenceiq.cloudbreak.cloud.template.compute.PreserveResourceException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.util.DeviceNameGenerator;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.common.model.AwsDiskType;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.ec2.model.CreateVolumeRequest;
import software.amazon.awssdk.services.ec2.model.CreateVolumeResponse;
import software.amazon.awssdk.services.ec2.model.DeleteVolumeRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.EbsInstanceBlockDeviceSpecification;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.InstanceBlockDeviceMappingSpecification;
import software.amazon.awssdk.services.ec2.model.ModifyInstanceAttributeRequest;
import software.amazon.awssdk.services.ec2.model.ModifyInstanceAttributeResponse;
import software.amazon.awssdk.services.ec2.model.Subnet;
import software.amazon.awssdk.services.ec2.model.TagSpecification;

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
    private AwsTaggingService awsTaggingService;

    @Inject
    private CommonAwsClient awsClient;

    @Inject
    private VolumeResourceCollector volumeResourceCollector;

    @Inject
    private AwsMethodExecutor awsMethodExecutor;

    @Inject
    private ResourceRetriever resourceRetriever;

    private final BiFunction<Volume, Boolean, InstanceBlockDeviceMappingSpecification> toInstanceBlockDeviceMappingSpecification = (volume, flag) -> {
        EbsInstanceBlockDeviceSpecification device = EbsInstanceBlockDeviceSpecification.builder()
                .volumeId(volume.getId())
                .deleteOnTermination(flag)
                .build();

        return InstanceBlockDeviceMappingSpecification.builder()
                .ebs(device)
                .deviceName(volume.getDevice())
                .build();
    };

    @Override
    public List<CloudResource> create(AwsContext context, CloudInstance instance, long privateId, AuthenticatedContext auth, Group group, Image image) {
        LOGGER.debug("Create volume resources for {} in group: {}", instance.getInstanceId(), group.getName());

        InstanceTemplate template = group.getReferenceInstanceTemplate();
        if (CollectionUtils.isEmpty(template.getVolumes())) {
            LOGGER.debug("No volume requested in group: {}", group.getName());
            return List.of();
        }

        List<CloudResource> computeResources = context.getComputeResources(privateId);
        Optional<CloudResource> reattachableVolumeSet = computeResources.stream()
                .filter(resource -> ResourceType.AWS_VOLUMESET.equals(resource.getType()))
                .findFirst();

        if (reattachableVolumeSet.isEmpty()) {
            reattachableVolumeSet = fetchCloudResourceFromDBIfAvailable(privateId, auth, group, computeResources);
        } else {
            LOGGER.debug("Reattachable volumeset found: {}", reattachableVolumeSet.get());
        }

        String subnetId = getSubnetId(context, instance);
        Optional<String> availabilityZone = getAvailabilityZone(context, instance);
        return List.of(reattachableVolumeSet.orElseGet(createVolumeSet(privateId, auth, group, subnetId, availabilityZone)));
    }

    Optional<CloudResource> fetchCloudResourceFromDBIfAvailable(long privateId, AuthenticatedContext auth, Group group,
            List<CloudResource> computeResources) {
        Optional<CloudResource> reattachableVolumeSet;
        CloudResource instanceResource = computeResources.stream()
                .filter(cr -> cr.getType().equals(ResourceType.AWS_INSTANCE))
                .findFirst()
                .orElseThrow(NotFoundException.notFound("AWS_INSTANCE", privateId));

        reattachableVolumeSet = findVolumeSet(auth, group, instanceResource, CommonStatus.CREATED);
        if (reattachableVolumeSet.isEmpty()) {
            reattachableVolumeSet = findVolumeSet(auth, group, instanceResource, CommonStatus.REQUESTED);
        }
        if (reattachableVolumeSet.isPresent()) {
            LOGGER.debug("Attachable volumeset present in the DB: {}, so use it", reattachableVolumeSet.get().getName());
        } else {
            LOGGER.debug("Volumeset cannot be find in DB for group: {}, privateId: {}", group.getName(), privateId);
        }
        return reattachableVolumeSet;
    }

    private Optional<CloudResource> findVolumeSet(AuthenticatedContext auth, Group group, CloudResource instanceResource, CommonStatus commonStatus) {
        return resourceRetriever.findAllByStatusAndTypeAndStackAndInstanceGroup(commonStatus, ResourceType.AWS_VOLUMESET, auth.getCloudContext().getId(),
                        group.getName())
                .stream()
                .filter(cr -> instanceResource.getInstanceId().equals(cr.getInstanceId()))
                .findFirst();
    }

    protected String getSubnetId(AwsContext context, CloudInstance cloudInstance) {
        return context.getNetworkResources().stream()
                .filter(cloudResource -> ResourceType.AWS_SUBNET.equals(cloudResource.getType())).findFirst().get().getName();
    }

    protected Optional<String> getAvailabilityZone(AwsContext context, CloudInstance cloudInstance) {
        return Optional.ofNullable(cloudInstance.getAvailabilityZone());
    }

    private Supplier<CloudResource> createVolumeSet(long privateId, AuthenticatedContext auth, Group group,
            String subnetId, Optional<String> availabilityZone) {
        return () -> {
            AwsResourceNameService resourceNameService = getResourceNameService();

            InstanceTemplate template = group.getReferenceInstanceTemplate();
            String groupName = group.getName();
            CloudContext cloudContext = auth.getCloudContext();
            String stackName = cloudContext.getName();

            String targetAvailabilityZone = getAvailabilityZoneFromSubnet(auth, subnetId, availabilityZone);
            LOGGER.info("Selected availability zone for volumeset: {}, group: {}", targetAvailabilityZone, groupName);
            return CloudResource.builder()
                    .withPersistent(true)
                    .withType(resourceType())
                    .withName(resourceNameService.resourceName(resourceType(), stackName, groupName, privateId))
                    .withAvailabilityZone(targetAvailabilityZone)
                    .withGroup(group.getName())
                    .withStatus(CommonStatus.REQUESTED)
                    .withParameters(Map.of(CloudResource.ATTRIBUTES, new VolumeSetAttributes.Builder()
                            .withAvailabilityZone(targetAvailabilityZone)
                            .withDeleteOnTermination(Boolean.TRUE)
                            .withVolumes(template.getVolumes().stream()
                                    .filter(vol -> !AwsDiskType.Ephemeral.value().equalsIgnoreCase(vol.getType()))
                                    .map(vol -> new Volume(null, null, vol.getSize(), vol.getType(), vol.getVolumeUsageType()))
                                    .collect(Collectors.toList()))
                            .build()))
                    .build();
        };
    }

    private String getAvailabilityZoneFromSubnet(AuthenticatedContext auth, String subnetId, Optional<String> availabilityZone) {
        if (availabilityZone.isPresent()) {
            LOGGER.debug("AZ is present: {}", availabilityZone.get());
            return availabilityZone.get();
        } else {
            String defaultAvailabilityZone = auth.getCloudContext().getLocation().getAvailabilityZone().value();
            AmazonEc2Client amazonEC2Client = getAmazonEC2Client(auth);
            DescribeSubnetsRequest describeSubnetsRequest = DescribeSubnetsRequest.builder().subnetIds(subnetId).build();
            DescribeSubnetsResponse describeSubnetsResponse = awsMethodExecutor
                    .execute(() -> amazonEC2Client.describeSubnets(describeSubnetsRequest), null);

            if (describeSubnetsResponse == null) {
                LOGGER.debug("Describe subnet is null, fallback to default: {}", defaultAvailabilityZone);
                return defaultAvailabilityZone;
            } else {
                return describeSubnetsResponse.subnets()
                        .stream()
                        .filter(subnet -> subnetId.equals(subnet.subnetId()))
                        .map(Subnet::availabilityZone)
                        .findFirst()
                        .orElseGet(() -> {
                            LOGGER.debug("Cannot find subnet in describe subnet response, fallback to default: {}", defaultAvailabilityZone);
                            return defaultAvailabilityZone;
                        });
            }
        }
    }

    @Override
    public List<CloudResource> build(AwsContext context, CloudInstance instance, long privateId, AuthenticatedContext auth, Group group,
            List<CloudResource> buildableResource, CloudStack cloudStack) throws Exception {
        LOGGER.debug("Create volumes on provider: {}", buildableResource.stream().map(CloudResource::getName).collect(Collectors.toList()));
        AmazonEc2Client client = getAmazonEC2Client(auth);

        Map<String, List<Volume>> volumeSetMap = Collections.synchronizedMap(new HashMap<>());

        List<Future<?>> futures = new ArrayList<>();
        boolean encryptedVolume = isEncryptedVolumeRequested(group);
        String volumeEncryptionKey = getVolumeEncryptionKey(group, encryptedVolume);
        TagSpecification tagSpecification = TagSpecification.builder()
                .resourceType(software.amazon.awssdk.services.ec2.model.ResourceType.VOLUME)
                .tags(awsTaggingService.prepareEc2Tags(cloudStack.getTags()))
                .build();
        List<CloudResource> requestedResources = buildableResource.stream()
                .filter(cloudResource -> CommonStatus.REQUESTED.equals(cloudResource.getStatus()))
                .collect(Collectors.toList());

        Long ephemeralCount = getEphemeralCount(group);

        LOGGER.debug("Start creating data volumes for stack: '{}' group: '{}'", auth.getCloudContext().getName(), group.getName());

        for (CloudResource resource : requestedResources) {
            volumeSetMap.put(resource.getName(), Collections.synchronizedList(new ArrayList<>()));

            VolumeSetAttributes volumeSet = resource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class);
            DeviceNameGenerator generator = new DeviceNameGenerator(DEVICE_NAME_TEMPLATE, ephemeralCount.intValue());
            futures.addAll(volumeSet.getVolumes().stream()
                    .map(createVolumeRequest(encryptedVolume, volumeEncryptionKey, tagSpecification, volumeSet))
                    .map(requestWithUsage -> intermediateBuilderExecutor.submit(() -> {
                        CreateVolumeRequest request = requestWithUsage.getFirst();
                        CreateVolumeResponse response = client.createVolume(request);
                        Volume volume = new Volume(response.volumeId(), generator.next(), request.size(), request.volumeTypeAsString(),
                                requestWithUsage.getSecond());
                        volumeSetMap.get(resource.getName()).add(volume);
                    }))
                    .collect(Collectors.toList()));
        }
        LOGGER.debug("Waiting for volumes creation requests");
        for (Future<?> future : futures) {
            future.get();
        }
        LOGGER.debug("Volume creation requests sent");
        String defaultAvailabilityZone = auth.getCloudContext().getLocation().getAvailabilityZone().value();
        return requestedResources.stream()
                .peek(resource -> {
                    List<Volume> volumes = volumeSetMap.get(resource.getName());
                    if (!CollectionUtils.isEmpty(volumes)) {
                        resource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class).setVolumes(volumes);
                    }
                })
                .map(copyResourceWithCreatedStatus(defaultAvailabilityZone))
                .collect(Collectors.toList());
    }

    @Override
    public CloudResource update(AwsContext context, CloudResource cloudResource, CloudInstance instance,
            AuthenticatedContext auth, CloudStack cloudStack) throws Exception {
        return null;
    }

    private Long getEphemeralCount(Group group) {
        Long ephemeralTemplateCount = group.getReferenceInstanceTemplate().getVolumes().stream()
                .filter(vol -> AwsDiskType.Ephemeral.value().equalsIgnoreCase(vol.getType())).count();
        if (ephemeralTemplateCount.equals(0L)) {
            return Optional.ofNullable(group.getReferenceInstanceTemplate()).map(InstanceTemplate::getTemporaryStorageCount).orElse(0L);
        } else {
            return ephemeralTemplateCount;
        }
    }

    private Function<CloudResource, CloudResource> copyResourceWithCreatedStatus(String defaultAvailabilityZone) {
        return resource -> CloudResource.builder()
                .withPersistent(true)
                .withGroup(resource.getGroup())
                .withType(resource.getType())
                .withStatus(CommonStatus.CREATED)
                .withName(resource.getName())
                .withAvailabilityZone(resource.getAvailabilityZone() == null ? defaultAvailabilityZone : resource.getAvailabilityZone())
                .withParameters(resource.getParameters())
                .build();
    }

    private boolean isEncryptedVolumeRequested(Group group) {
        return new AwsInstanceView(group.getReferenceInstanceTemplate()).isEncryptedVolumes();
    }

    private String getVolumeEncryptionKey(Group group, boolean encryptedVolume) {
        AwsInstanceView awsInstanceView = new AwsInstanceView(group.getReferenceInstanceTemplate());
        return encryptedVolume && awsInstanceView.isKmsCustom() ? awsInstanceView.getKmsKey() : null;
    }

    private Function<Volume, Pair<CreateVolumeRequest, CloudVolumeUsageType>> createVolumeRequest(boolean encryptedVolume,
            String volumeEncryptionKey, TagSpecification tagSpecification, VolumeSetAttributes volumeSet) {
        return volume -> {
            CreateVolumeRequest createVolumeRequest = CreateVolumeRequest.builder()
                    .availabilityZone(volumeSet.getAvailabilityZone())
                    .size(volume.getSize())
                    .snapshotId(null)
                    .tagSpecifications(tagSpecification)
                    .volumeType(volume.getType())
                    .encrypted(encryptedVolume)
                    .kmsKeyId(volumeEncryptionKey)
                    .build();
            return Pair.of(createVolumeRequest, volume.getCloudVolumeUsageType());
        };
    }

    @Override
    public CloudResource delete(AwsContext context, AuthenticatedContext auth, CloudResource resource) throws PreserveResourceException {
        LOGGER.debug("Set delete on termination to true, on instances");
        VolumeSetAttributes volumeSetAttributes = resource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class);
        List<CloudResourceStatus> cloudResourceStatuses = checkResources(ResourceType.AWS_VOLUMESET, context, auth, List.of(resource));

        boolean anyDeleted = cloudResourceStatuses.stream().map(CloudResourceStatus::getStatus).anyMatch(DELETED::equals);
        AmazonEc2Client client = getAmazonEC2Client(auth);
        if (!volumeSetAttributes.getDeleteOnTermination() && !anyDeleted) {
            LOGGER.debug("Volumes will be preserved.");
            resource.setStatus(CommonStatus.DETACHED);
            volumeSetAttributes.setDeleteOnTermination(Boolean.TRUE);
            turnOffDeleteOnterminationOnAttachedVolumes(resource, cloudResourceStatuses, client);
            resource.putParameter(CloudResource.ATTRIBUTES, volumeSetAttributes);
            resourceNotifier.notifyUpdate(resource, auth.getCloudContext());
            throw new PreserveResourceException("Resource will be preserved for later reattachment.");
        }
        deleteOrphanedVolumes(cloudResourceStatuses, client);
        turnOnDeleteOnterminationOnAttachedVolumes(resource, cloudResourceStatuses, client);

        return null;
    }

    private void turnOnDeleteOnterminationOnAttachedVolumes(CloudResource resource, List<CloudResourceStatus> cloudResourceStatuses,
            AmazonEc2Client client) {
        modifyDeleteOnterminationOnAttachedVolumes(resource, cloudResourceStatuses, Boolean.TRUE, client);
    }

    private void turnOffDeleteOnterminationOnAttachedVolumes(CloudResource resource, List<CloudResourceStatus> cloudResourceStatuses,
            AmazonEc2Client client) {
        modifyDeleteOnterminationOnAttachedVolumes(resource, cloudResourceStatuses, Boolean.FALSE, client);
    }

    private void modifyDeleteOnterminationOnAttachedVolumes(CloudResource resource, List<CloudResourceStatus> cloudResourceStatuses,
            Boolean deleteOnTermination, AmazonEc2Client client) {
        String instanceId = resource.getInstanceId();
        if (StringUtils.isNotEmpty(instanceId)) {
            List<InstanceBlockDeviceMappingSpecification> attachedDeviceMappingSpecifications =
                    getDeviceMappingSpecifications(cloudResourceStatuses, deleteOnTermination, EnumSet.of(ATTACHED));
            if (!attachedDeviceMappingSpecifications.isEmpty()) {
                changeDeleteOnTermination(deleteOnTermination, client, instanceId, attachedDeviceMappingSpecifications);
            } else {
                LOGGER.info("No device mapping specification found for instance '{}', skipping the modify instance attributes call to AWS.", instanceId);
            }
            List<InstanceBlockDeviceMappingSpecification> deletedAndDetachedDeviceMappingSpecifications =
                    getDeviceMappingSpecifications(cloudResourceStatuses, deleteOnTermination, EnumSet.of(DELETED, CREATED));
            if (!deletedAndDetachedDeviceMappingSpecifications.isEmpty() && deleteOnTermination) {
                for (InstanceBlockDeviceMappingSpecification specification : deletedAndDetachedDeviceMappingSpecifications) {
                    try {
                        changeDeleteOnTermination(deleteOnTermination, client, instanceId, List.of(specification));
                    } catch (AwsServiceException e) {
                        LOGGER.debug("Volume '{}' is already deleted.", specification.deviceName(), e);
                    }
                }
            }
        } else {
            LOGGER.info("No instance id found for volume set resource, skipping the modify instance attributes call to AWS.");
        }
    }

    private void changeDeleteOnTermination(Boolean deleteOnTermination, AmazonEc2Client client,
            String instanceId, List<InstanceBlockDeviceMappingSpecification> deviceMappingSpecifications) {
        ModifyInstanceAttributeRequest modifyInstanceAttributeRequest = ModifyInstanceAttributeRequest.builder()
                .instanceId(instanceId)
                .blockDeviceMappings(deviceMappingSpecifications)
                .build();
        ModifyInstanceAttributeResponse modifyIdentityIdFormatResponse = awsMethodExecutor.execute(
                () -> client.modifyInstanceAttribute(modifyInstanceAttributeRequest), null);
                String result = instanceId + " not found on the provider.";
                if (modifyIdentityIdFormatResponse != null) {
                    result = modifyIdentityIdFormatResponse.toString();
        }
        LOGGER.info("Delete on termination set to '{}' on instance '{}'. {}", deleteOnTermination, instanceId, result);
    }

    private List<InstanceBlockDeviceMappingSpecification> getDeviceMappingSpecifications(List<CloudResourceStatus> cloudResourceStatuses,
            Boolean deleteOnTermination, Set<ResourceStatus> resourceStatus) {
        return cloudResourceStatuses.stream()
                .filter(cloudResourceStatus -> resourceStatus.contains(cloudResourceStatus.getStatus()))
                .map(CloudResourceStatus::getCloudResource)
                .map(cloudResource -> cloudResource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class))
                .map(VolumeSetAttributes::getVolumes)
                .flatMap(List::stream)
                .map(volume -> toInstanceBlockDeviceMappingSpecification.apply(volume, deleteOnTermination))
                .collect(Collectors.toList());
    }

    private void deleteOrphanedVolumes(List<CloudResourceStatus> cloudResourceStatuses, AmazonEc2Client client) {
        LOGGER.debug("Deleting orphan volumes");
        cloudResourceStatuses.stream()
                .filter(cloudResourceStatus -> CREATED.equals(cloudResourceStatus.getStatus()))
                .map(CloudResourceStatus::getCloudResource)
                .map(cloudResource -> cloudResource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class))
                .map(VolumeSetAttributes::getVolumes)
                .flatMap(List::stream)
                .map(VolumeSetAttributes.Volume::getId)
                .map(volumeId -> DeleteVolumeRequest.builder().volumeId(volumeId).build())
                .forEach(request -> deleteVolumeSilentlyByDeleteVolumeRequest(client, request));
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.AWS_VOLUMESET;
    }

    @Override
    protected List<CloudResourceStatus> checkResources(ResourceType type, AwsContext context, AuthenticatedContext auth, Iterable<CloudResource> resources) {
        AmazonEc2Client client = getAmazonEC2Client(auth);

        Pair<List<String>, List<CloudResource>> volumes = volumeResourceCollector.getVolumeIdsByVolumeResources(resources, resourceType(),
                volumeSetAttributes());

        DescribeVolumesRequest.Builder describeVolumesRequestBuilder = DescribeVolumesRequest.builder();
        if (!CollectionUtils.isEmpty(volumes.getFirst())) {
            describeVolumesRequestBuilder.volumeIds(volumes.getFirst());
        }
        DescribeVolumesRequest describeVolumesRequest = describeVolumesRequestBuilder.build();
        LOGGER.debug("Going to describe volume(s) with id(s): [{}]", String.join(",", describeVolumesRequest.volumeIds()));
        AtomicReference<ResourceStatus> volumeSetStatus = new AtomicReference<>();
        try {
            DescribeVolumesResponse response = client.describeVolumes(describeVolumesRequest);
            volumeSetStatus.set(getResourceStatus(response));
        } catch (Ec2Exception e) {
            if (!"InvalidVolume.NotFound".equals(e.awsErrorDetails().errorCode())) {
                throw e;
            }
            LOGGER.info("The volume doesn't need to be deleted as it does not exist on the provider side. Reason: {}", e.getMessage());
            volumeSetStatus.set(DELETED);
        }
        LOGGER.debug("[{}] volume set status is {}", String.join(",", volumes.getFirst()), volumeSetStatus);
        return volumes.getSecond().stream()
                .map(resource -> new CloudResourceStatus(resource, volumeSetStatus.get()))
                .collect(Collectors.toList());
    }

    private void deleteVolumeSilentlyByDeleteVolumeRequest(AmazonEc2Client client, DeleteVolumeRequest request) {
        LOGGER.debug("About to delete the following volume: {}", request.toString());
        try {
            client.deleteVolume(request);
        } catch (AwsServiceException e) {
            LOGGER.debug(String.format("Exception during aws volume (%s) deletion.", request.volumeId()), e);
        }
    }

    private ResourceStatus getResourceStatus(DescribeVolumesResponse response) {
        try {
            return response.volumes()
                    .stream()
                    .peek(volume -> LOGGER.debug("State of volume {} is {}", volume.volumeId(), volume.state()))
                    .map(software.amazon.awssdk.services.ec2.model.Volume::stateAsString)
                    .map(toResourceStatus())
                    .reduce(ATTACHED, resourceStatusReducer());
        } catch (Ec2Exception e) {
            LOGGER.debug("Obtaining volume status was not successful due to the following error: " + e.awsErrorDetails().errorCode(), e);
            return "InvalidVolume.NotFound".equals(e.awsErrorDetails().errorCode()) ? DELETED : FAILED;
        }
    }

    private BinaryOperator<ResourceStatus> resourceStatusReducer() {
        return (statusA, statusB) -> {
            List<ResourceStatus> statuses = List.of(statusA, statusB);
            if (statuses.contains(DELETED)) {
                return DELETED;
            } else if (statuses.contains(IN_PROGRESS)) {
                return IN_PROGRESS;
            } else if (statuses.contains(CREATED)) {
                return CREATED;
            }

            return ATTACHED;
        };
    }

    private Function<String, ResourceStatus> toResourceStatus() {
        return state -> {
            switch (state) {
                case "available":
                    return CREATED;
                case "in-use":
                    return ATTACHED;
                case "deleting":
                    return DELETED;
                case "creating":
                default:
                    return IN_PROGRESS;
            }
        };
    }

    private Function<CloudResource, VolumeSetAttributes> volumeSetAttributes() {
        return volumeSet -> volumeSet.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class);
    }

    private AmazonEc2Client getAmazonEC2Client(AuthenticatedContext auth) {
        AwsCredentialView credentialView = new AwsCredentialView(auth.getCloudCredential());
        String regionName = auth.getCloudContext().getLocation().getRegion().value();
        return awsClient.createEc2Client(credentialView, regionName);
    }

    @Override
    public int order() {
        return 1;
    }

}
