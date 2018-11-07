package com.sequenceiq.cloudbreak.cloud.aws.resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateVolumeRequest;
import com.amazonaws.services.ec2.model.CreateVolumeResult;
import com.amazonaws.services.ec2.model.DescribeVolumesRequest;
import com.amazonaws.services.ec2.model.DescribeVolumesResult;
import com.amazonaws.services.ec2.model.EbsInstanceBlockDeviceSpecification;
import com.amazonaws.services.ec2.model.InstanceBlockDeviceMappingSpecification;
import com.amazonaws.services.ec2.model.ModifyInstanceAttributeRequest;
import com.amazonaws.services.ec2.model.ModifyInstanceAttributeResult;
import com.amazonaws.services.ec2.model.TagSpecification;
import com.sequenceiq.cloudbreak.cloud.aws.AwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.AwsTagPreparationService;
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
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.common.type.CommonStatus;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

@Component
public class AwsVolumeResourceBuilder extends AbstractAwsComputeBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsVolumeResourceBuilder.class);

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

    @Override
    public List<CloudResource> create(AwsContext context, long privateId, AuthenticatedContext auth, Group group, Image image) {
        LOGGER.info("Create volume resources");
        AwsResourceNameService resourceNameService = getResourceNameService();

        CloudInstance instance = group.getReferenceInstanceConfiguration();
        InstanceTemplate template = instance.getTemplate();
        Volume volumeTemplate = template.getVolumes().iterator().next();
        String groupName = group.getName();
        CloudContext cloudContext = auth.getCloudContext();
        String stackName = cloudContext.getName();
        return List.of(new Builder()
                .persistent(true)
                .type(resourceType())
                .name(resourceNameService.resourceName(resourceType(), stackName, groupName, privateId))
                .group(group.getName())
                .status(CommonStatus.CREATED)
                .params(Map.of(CloudResource.ATTRIBUTES, new VolumeSetAttributes.Builder()
                        .withVolumeSize(volumeTemplate.getSize())
                        .withVolumeType(volumeTemplate.getType())
                        .withAvailabilityZone(auth.getCloudContext().getLocation().getAvailabilityZone().value())
                        .withVolumes(template.getVolumes().stream().map(vol -> new VolumeSetAttributes.Volume(null, vol.getMount(), null, null))
                                .collect(Collectors.toList()))
                        .build()))
                .build());
    }

    @Override
    public List<CloudResource> build(AwsContext context, long privateId, AuthenticatedContext auth, Group group,
            List<CloudResource> buildableResource, CloudStack cloudStack) throws Exception {
        LOGGER.info("Create volumes on provider");
        AmazonEC2Client client = getAmazonEC2Client(auth);

        Map<String, List<VolumeSetAttributes.Volume>> volumeSetMap = Collections.synchronizedMap(new HashMap<>());

        List<Future<?>> futures = new ArrayList<>();
        String snapshotId = getEbsSnapshotIdIfNeeded(auth, cloudStack, group);
        TagSpecification tagSpecification = new TagSpecification()
                .withResourceType(com.amazonaws.services.ec2.model.ResourceType.Volume)
                .withTags(awsTagPreparationService.prepareEc2Tags(auth, cloudStack.getTags()));
        for (CloudResource resource : buildableResource) {
            volumeSetMap.put(resource.getName(), Collections.synchronizedList(new ArrayList<>()));

            VolumeSetAttributes volumeSet = resource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class);
            futures.addAll(volumeSet.getVolumes().stream()
                    .map(createVolumeRequest(snapshotId, tagSpecification, volumeSet))
                    .map(request -> intermediateBuilderExecutor.submit(() -> {
                        CreateVolumeResult result = client.createVolume(request);
                        String volumeId = result.getVolume().getVolumeId();
                        volumeSetMap.get(resource.getName()).add(new VolumeSetAttributes.Volume(volumeId, null, null, null));
                    }))
                    .collect(Collectors.toList()));
        }

        for (Future<?> future : futures) {
            future.get();
        }

        buildableResource.forEach(resource ->
                resource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class).setVolumes(volumeSetMap.get(resource.getName())));

        return buildableResource;
    }

    private Function<VolumeSetAttributes.Volume, CreateVolumeRequest> createVolumeRequest(String snapshotId, TagSpecification tagSpecification,
            VolumeSetAttributes volumeSet) {
        return volume -> new CreateVolumeRequest()
                .withAvailabilityZone(volumeSet.getAvailabilityZone())
                .withSize(volumeSet.getVolumeSize())
                .withSnapshotId(snapshotId)
                .withTagSpecifications(tagSpecification)
                .withVolumeType(volumeSet.getVolumeType());
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
    public CloudResource delete(AwsContext context, AuthenticatedContext auth, CloudResource resource) {
        LOGGER.info("Set delete on termination to true, on instances");
        VolumeSetAttributes volumeSetAttributes = resource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class);
        List<InstanceBlockDeviceMappingSpecification> deviceMappingSpecifications = volumeSetAttributes
                .getVolumes().stream()
                .map(volume -> {
                    EbsInstanceBlockDeviceSpecification device = new EbsInstanceBlockDeviceSpecification()
                            .withVolumeId(volume.getId())
                            .withDeleteOnTermination(true);

                    return new InstanceBlockDeviceMappingSpecification()
                            .withEbs(device)
                            .withDeviceName(volume.getDevice());
                })
                .collect(Collectors.toList());
        ModifyInstanceAttributeRequest modifyInstanceAttributeRequest = new ModifyInstanceAttributeRequest()
                .withInstanceId(resource.getInstanceId())
                .withBlockDeviceMappings(deviceMappingSpecifications);

        AmazonEC2Client client = getAmazonEC2Client(auth);
        LOGGER.debug("Volume delete request {}", modifyInstanceAttributeRequest);
        ModifyInstanceAttributeResult modifyIdentityIdFormatResult = client.modifyInstanceAttribute(modifyInstanceAttributeRequest);
        LOGGER.debug("Volume delete result {}", modifyIdentityIdFormatResult);
        return null;
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.AWS_VOLUMESET;
    }

    @Override
    protected List<CloudResourceStatus> checkResources(ResourceType type, AwsContext context, AuthenticatedContext auth, Iterable<CloudResource> resources) {

        AmazonEC2Client client = getAmazonEC2Client(auth);
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
        ResourceStatus volumeSetStatus = result.getVolumes().stream()
                .map(com.amazonaws.services.ec2.model.Volume::getState)
                .allMatch("available"::equals) ? ResourceStatus.CREATED : ResourceStatus.IN_PROGRESS;
        return volumeResources.stream()
                .map(resource -> new CloudResourceStatus(resource, volumeSetStatus))
                .collect(Collectors.toList());
    }

    private Function<CloudResource, VolumeSetAttributes> volumeSetAttributes() {
        return volumeSet -> volumeSet.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class);
    }

    private AmazonEC2Client getAmazonEC2Client(AuthenticatedContext auth) {
        AwsCredentialView credentialView = new AwsCredentialView(auth.getCloudCredential());
        String regionName = auth.getCloudContext().getLocation().getRegion().value();
        return awsClient.createAccess(credentialView, regionName);
    }

    @Override
    public int order() {
        return 1;
    }
}
