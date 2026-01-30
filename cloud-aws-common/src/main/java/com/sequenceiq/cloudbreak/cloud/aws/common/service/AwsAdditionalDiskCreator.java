package com.sequenceiq.cloudbreak.cloud.aws.common.service;

import static com.sequenceiq.cloudbreak.cloud.model.CloudInstance.FQDN;
import static com.sequenceiq.cloudbreak.cloud.model.CloudResource.PRIVATE_ID;
import static java.lang.String.format;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.google.api.client.util.Lists;
import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeStatus;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.util.DeviceNameGenerator;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.services.ec2.model.CreateVolumeRequest;
import software.amazon.awssdk.services.ec2.model.CreateVolumeResponse;
import software.amazon.awssdk.services.ec2.model.DeleteVolumeRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.TagSpecification;
import software.amazon.awssdk.services.ec2.model.VolumeState;

@Service
public class AwsAdditionalDiskCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsAdditionalDiskCreator.class);

    private static final String DEVICE_NAME_TEMPLATE = "/dev/xvd%s";

    @Inject
    private AwsResourceNameService awsResourceNameService;

    @Inject
    private AwsCommonDiskUtilService awsCommonDiskUtilService;

    @Inject
    private AwsCommonDiskUpdateService awsCommonDiskUpdateService;

    @Inject
    private CommonAwsClient commonAwsClient;

    public List<CloudResource> createVolumes(AuthenticatedContext authenticatedContext, Group group, VolumeSetAttributes.Volume volumeRequest,
            CloudStack cloudStack, int volToAddPerInstance, List<CloudResource> cloudResources) throws CloudbreakServiceException {
        try {
            LOGGER.info("Creating additional EBS volumes with : {} for group: {}", volumeRequest, group.getName());
            if (CollectionUtils.isEmpty(cloudResources)) {
                LOGGER.info("There are no attached EBS volumes in the group. So creating resources!");
                cloudResources = createNewResource(group, authenticatedContext);
            }
            boolean encryptedVolume = awsCommonDiskUtilService.isEncryptedVolumeRequested(group);
            String volumeEncryptionKey = awsCommonDiskUtilService.getVolumeEncryptionKey(group, encryptedVolume);
            TagSpecification tagSpecification = awsCommonDiskUtilService.getTagSpecification(cloudStack);
            AmazonEc2Client client = commonAwsClient.createEc2Client(authenticatedContext);
            int attachedVolumesCount = group.getReferenceInstanceTemplate().getVolumes().size() +
                    group.getReferenceInstanceTemplate().getTemporaryStorageCount().intValue();
            CreateVolumesRequest createVolumesRequest = new CreateVolumesRequest(cloudResources, attachedVolumesCount, volToAddPerInstance, volumeRequest,
                    tagSpecification, volumeEncryptionKey, encryptedVolume);
            Map<String, List<VolumeSetAttributes.Volume>> volumeSetMap = createAllVolumes(createVolumesRequest, client, group);
            cloudResources.forEach(resource -> {
                List<VolumeSetAttributes.Volume> volumes = volumeSetMap.get(resource.getName());
                if (!CollectionUtils.isEmpty(volumes)) {
                    VolumeSetAttributes volumeSetAttributes = resource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class);
                    try {
                        if (volumeSetAttributes.getDiscoveryFQDN() != null) {
                            String fqdnForResource = getFqdnForResource(group, resource);
                            volumeSetAttributes.setDiscoveryFQDN(fqdnForResource);
                        }
                    } catch (CloudbreakException e) {
                        LOGGER.warn("Exception while getting fqdn for resource: {}", resource, e);
                    }
                    volumeSetAttributes.getVolumes().addAll(volumes);
                    resource.setStatus(CommonStatus.CREATED);
                }
            });
            LOGGER.info("Created resources with additional volumes: {}", cloudResources);
            return cloudResources;
        } catch (Exception ex) {
            LOGGER.warn("Exception while creating new volumes: {}", ex.getMessage());
            throw new CloudbreakServiceException(ex);
        }
    }

    private List<CloudResource> createNewResource(Group group, AuthenticatedContext ac) {
        List<CloudResource> cloudResources = new ArrayList<>();
        for (CloudInstance instance : group.getInstances()) {
            Long privateId = instance.getTemplate().getPrivateId();
            String resourceName = awsResourceNameService.attachedDisk(ac.getCloudContext().getName(), group.getName(), privateId);
            CloudResource resource = CloudResource.builder().withGroup(group.getName()).withInstanceId(instance.getInstanceId()).withName(resourceName)
                    .withType(ResourceType.AWS_VOLUMESET).withStatus(CommonStatus.REQUESTED).withAvailabilityZone(instance.getAvailabilityZone())
                    .withParameters(Map.of(CloudResource.ATTRIBUTES, new VolumeSetAttributes.Builder()
                                    .withAvailabilityZone(instance.getAvailabilityZone())
                                    .withDeleteOnTermination(Boolean.TRUE)
                                    .withVolumes(new ArrayList<>())
                                    .withDiscoveryFQDN(instance.getParameter(FQDN, String.class))
                                    .build(),
                            PRIVATE_ID, privateId))
                    .build();
            cloudResources.add(resource);
        }
        LOGGER.info("Created new resources by adding block storage flow: {}", cloudResources);
        return cloudResources;
    }

    private Map<String, List<VolumeSetAttributes.Volume>> createAllVolumes(CreateVolumesRequest createVolumesRequest, AmazonEc2Client client, Group group)
            throws Exception {
        Map<String, List<VolumeSetAttributes.Volume>> volumeSetMap = new HashMap<>();
        List<String> volumeIdsCreated = new ArrayList<>();
        int requestVolsToAddPerInstance = createVolumesRequest.getVolToAddPerInstance();
        List<String> fqdnForAllResources = group.getInstances().stream().map(instance -> instance.getParameters().get("FQDN").toString()).toList();
        Map<String, Integer> newVolumesToCreateByFqdn = getNewVolumesToCreateCount(requestVolsToAddPerInstance, fqdnForAllResources, client);
        for (CloudResource resource: createVolumesRequest.getCloudResources()) {
            String fqdn = getFqdnForResource(group, resource);
            int attachedVolumesCount = createVolumesRequest.getAttachedVolumesCount();
            int remainingVolumesPerInstance = newVolumesToCreateByFqdn.get(fqdn);
            DeviceNameGenerator generator = new DeviceNameGenerator(DEVICE_NAME_TEMPLATE, attachedVolumesCount + 1);
            volumeSetMap.put(resource.getName(), Lists.newArrayList());
            if (remainingVolumesPerInstance < requestVolsToAddPerInstance) {
                List<VolumeSetAttributes.Volume> orphanedVolumes = getOrphanedVolumes(client, fqdn, createVolumesRequest, generator);
                List<String> volumeIdsSavedToDB = resource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class).getVolumes()
                        .stream().map(VolumeSetAttributes.Volume::getId).toList();
                for (VolumeSetAttributes.Volume volume : orphanedVolumes) {
                    if (!volumeIdsSavedToDB.contains(volume.getId())) {
                        volumeSetMap.get(resource.getName()).add(volume);
                    }
                }
            }
            while (remainingVolumesPerInstance-- > 0) {
                LOGGER.info("Creating volumes for create volume request: {}, for instance: {}", createVolumesRequest, fqdn);
                String availabilityZone = null != resource.getAvailabilityZone() ? resource.getAvailabilityZone() : "";
                createVolumesRequest.setAvailabilityZone(availabilityZone);
                String newVolumeIdCreated = "";
                try {
                    newVolumeIdCreated = createAndAddVolumes(createVolumesRequest, client, volumeSetMap, resource, fqdn, generator);
                } catch (Exception ex) {
                    String exceptionMessage = format("Error while creating and attaching disks to the instance: %s, exception: %s", resource.getInstanceId(),
                            ex.getMessage());
                    LOGGER.warn(exceptionMessage);
                    deleteOrphanedVolumes(client, volumeIdsCreated);
                    throw new CloudbreakServiceException(exceptionMessage);
                }
                volumeIdsCreated.add(newVolumeIdCreated);
            }
        }
        awsCommonDiskUpdateService.pollVolumeStates(client, volumeIdsCreated);
        return volumeSetMap;
    }

    private String getFqdnForResource(Group group, CloudResource resource) throws CloudbreakException {
        return (String) group.getInstances().stream().filter(instance -> instance.getInstanceId().equals(resource.getInstanceId()))
                .map(instance -> instance.getParameters().get(FQDN)).findFirst()
                .orElseThrow(() -> new CloudbreakException(format("Instance ID :%s does not have any fqdn attached.", resource.getInstanceId())));
    }

    private String createAndAddVolumes(CreateVolumesRequest createVolumesRequest, AmazonEc2Client client, Map<String,
            List<VolumeSetAttributes.Volume>> volumeSetMap, CloudResource resource, String fqdn, DeviceNameGenerator generator) {
        TagSpecification tagSpecification = awsCommonDiskUtilService.addAdditionalTags(Map.of("created-for", fqdn),
                createVolumesRequest.getTagSpecification());
        CreateVolumeResponse createResponse = createVolume(createVolumesRequest.getVolumeRequest(), tagSpecification,
                createVolumesRequest.getVolumeEncryptionKey(), createVolumesRequest.isEncryptedVolume(),
                createVolumesRequest.getAvailabilityZone(), client);
        LOGGER.debug("Response for create volume : {}", createResponse);
        String deviceName = generator.next();
        VolumeSetAttributes.Volume volume = new VolumeSetAttributes.Volume(createResponse.volumeId(), deviceName,
                createVolumesRequest.getVolumeRequest().getSize(), createVolumesRequest.getVolumeRequest().getType(),
                createVolumesRequest.getVolumeRequest().getCloudVolumeUsageType());
        volume.setCloudVolumeStatus(CloudVolumeStatus.CREATED);
        volumeSetMap.get(resource.getName()).add(volume);
        return createResponse.volumeId();
    }

    private void deleteOrphanedVolumes(AmazonEc2Client client, List<String> volumeIdsCreated) {
        awsCommonDiskUpdateService.pollVolumeStates(client, volumeIdsCreated);
        volumeIdsCreated.forEach(volume -> {
            LOGGER.debug("Deleting volume after failed create - {}", volume);
            DeleteVolumeRequest deleteVolumeRequest = DeleteVolumeRequest.builder().volumeId(volume).build();
            try {
                client.deleteVolume(deleteVolumeRequest);
            } catch (Exception ex) {
                LOGGER.warn("Exception while removing orphaned volume - {}", deleteVolumeRequest);
            }
        });
    }

    private CreateVolumeResponse createVolume(VolumeSetAttributes.Volume volume, TagSpecification tagSpecification, String volumeEncryptionKey,
            boolean encryptedVolume, String availabilityZone, AmazonEc2Client client) {
        CreateVolumeRequest createVolumeRequest =  awsCommonDiskUtilService.createVolumeRequest(volume, tagSpecification, volumeEncryptionKey,
                encryptedVolume, availabilityZone);
        LOGGER.info("Sending create volume request : {}", createVolumeRequest);
        return client.createVolume(createVolumeRequest);
    }

    private Map<String, Integer> getNewVolumesToCreateCount(int volumesToAddPerInstance, List<String> fqdns, AmazonEc2Client client) {
        Map<String, List<software.amazon.awssdk.services.ec2.model.Volume>> availableVolumesMap =
                awsCommonDiskUpdateService.getVolumesInAvailableStatusByTagsFilter(client, Map.of("tag:created-for", fqdns));
        Map<String, Integer> availableVolumesMapByFqdn = new HashMap<>();
        for (String fqdn: fqdns) {
            availableVolumesMapByFqdn.put(fqdn, Math.max(volumesToAddPerInstance - availableVolumesMap.getOrDefault(fqdn, new ArrayList<>()).size(), 0));
        }
        return availableVolumesMapByFqdn;
    }

    private List<VolumeSetAttributes.Volume> getOrphanedVolumes(AmazonEc2Client amazonEC2Client,
            String fqdn, CreateVolumesRequest createVolumesRequest, DeviceNameGenerator generator) {
        List<Filter> filterRequest = awsCommonDiskUpdateService.getFiltersForDescribeVolumeRequest(Map.of("tag:created-for", List.of(fqdn)));
        DescribeVolumesRequest describeVolumesRequest = DescribeVolumesRequest.builder().filters(filterRequest).build();
        DescribeVolumesResponse volumesResponse = amazonEC2Client.describeVolumes(describeVolumesRequest);
        List<VolumeSetAttributes.Volume> volumes = Lists.newArrayList();
        if (volumesResponse.hasVolumes()) {
            volumes = volumesResponse.volumes().stream().filter(vol -> VolumeState.AVAILABLE.equals(vol.state()))
                    .map(vol -> convertEc2VolumeToVolume(vol, createVolumesRequest, generator.next())).toList();
        }
        return volumes;
    }

    private VolumeSetAttributes.Volume convertEc2VolumeToVolume(software.amazon.awssdk.services.ec2.model.Volume volResponse,
            CreateVolumesRequest createVolumesRequest, String deviceName) {
        VolumeSetAttributes.Volume volume = new VolumeSetAttributes.Volume(volResponse.volumeId(), deviceName, volResponse.size(),
                volResponse.volumeType().toString(), createVolumesRequest.getVolumeRequest().getCloudVolumeUsageType());
        volume.setCloudVolumeStatus(CloudVolumeStatus.CREATED);
        return volume;
    }
}