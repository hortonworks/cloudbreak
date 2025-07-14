package com.sequenceiq.cloudbreak.cloud.aws.common.service;

import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

import software.amazon.awssdk.services.ec2.model.AttachVolumeRequest;
import software.amazon.awssdk.services.ec2.model.AttachVolumeResponse;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.Volume;
import software.amazon.awssdk.services.ec2.model.VolumeAttachment;
import software.amazon.awssdk.services.ec2.model.VolumeState;

@Component
public class AwsAdditionalDiskAttachmentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsAdditionalDiskAttachmentService.class);

    @Inject
    private CommonAwsClient commonAwsClient;

    @Inject
    private AwsCommonDiskUpdateService awsCommonDiskUpdateService;

    @Inject
    @Qualifier("intermediateBuilderExecutor")
    private AsyncTaskExecutor intermediateBuilderExecutor;

    public Map<String, Integer> getAttachedVolumeCountPerInstance(AuthenticatedContext authenticatedContext, Collection<String> instanceIds) {
        AmazonEc2Client client = commonAwsClient.createEc2Client(authenticatedContext);
        DescribeInstancesResponse describeInstancesResponse = client.describeInstances(DescribeInstancesRequest.builder()
                .instanceIds(instanceIds)
                .build());
        Map<String, String> instanceRootDeviceNames = describeInstancesResponse.reservations().stream()
                .map(Reservation::instances)
                .flatMap(Collection::stream)
                .collect(Collectors.toMap(Instance::instanceId, Instance::rootDeviceName));
        DescribeVolumesResponse describeVolumesResponse = client.describeVolumes(DescribeVolumesRequest.builder()
                .filters(Filter.builder()
                        .name("attachment.instance-id")
                        .values(instanceIds)
                        .build())
                .build());
        Map<String, Integer> result = describeVolumesResponse.volumes().stream()
                .filter(v -> {
                    // Filter root volumes
                    VolumeAttachment volumeAttachment = v.attachments().getFirst();
                    String rootDeviceNameOfInstance = instanceRootDeviceNames.get(volumeAttachment.instanceId());
                    return !Objects.equals(rootDeviceNameOfInstance, volumeAttachment.device());
                })
                .collect(Collectors.groupingBy(v -> v.attachments().getFirst().instanceId(), Collectors.reducing(0, v -> 1, Integer::sum)));
        instanceIds.forEach(i -> result.computeIfAbsent(i, k -> 0));
        return result;
    }

    public void attachAllVolumes(AuthenticatedContext authenticatedContext, List<CloudResource> cloudResources) {
        AmazonEc2Client client = commonAwsClient.createEc2Client(authenticatedContext);
        Map<String, List<VolumeSetAttributes.Volume>> volumeSetMap = getInstanceVolumeIdsMap(cloudResources);
        List<String> volumeIds = cloudResources.stream()
                .map(resource -> resource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class).getVolumes())
                .flatMap(Collection::stream).map(VolumeSetAttributes.Volume::getId).toList();
        Map<String, VolumeState> volumeStates = awsCommonDiskUpdateService.getVolumeStates(volumeIds, client, Map.of());
        List<AttachVolumeRequest> attachVolumeRequests = new ArrayList<>();
        Set<String> volumeIdsToAttach = new HashSet<>();
        List<String> instanceIds = new ArrayList<>();
        for (CloudResource resource : cloudResources) {
            List<VolumeSetAttributes.Volume> createdVolumes = volumeSetMap.get(resource.getInstanceId());
            createdVolumes.forEach(volume -> {
                if (VolumeState.AVAILABLE.equals(volumeStates.get(volume.getId()))) {
                    LOGGER.debug("Attach volume request : {}", volume);
                    volumeIdsToAttach.add(volume.getId());
                    instanceIds.add(resource.getInstanceId());
                    attachVolumeRequests.add(createAttachVolumeRequest(resource.getInstanceId(), volume.getId(), volume.getDevice()));
                }
            });
        }
        List<Future<AttachVolumeResponse>> futures = attachVolumeRequests.stream()
                .map(request -> intermediateBuilderExecutor.submit(() -> client.attachVolume(request))).toList();
        LOGGER.debug("Waiting for attach volumes request");
        for (Future<AttachVolumeResponse> future : futures) {
            try {
                future.get();
            } catch (Throwable throwable) {
                LOGGER.info("Attachment failed with error.", throwable);
                checkVolumeAttachmentStatus(instanceIds, client, volumeIdsToAttach, throwable);
            }
        }
        LOGGER.debug("Attach volume requests sent");
    }

    private Map<String, List<VolumeSetAttributes.Volume>> getInstanceVolumeIdsMap(List<CloudResource> cloudResources) {
        return cloudResources.stream().filter(res -> null != res.getInstanceId())
                .collect(toMap(CloudResource::getInstanceId, resource -> resource.getParameter(CloudResource.ATTRIBUTES,
                        VolumeSetAttributes.class).getVolumes()));
    }

    protected AttachVolumeRequest createAttachVolumeRequest(String instanceId, String volumeId, String deviceName) {
        LOGGER.debug("Creating attach volume request for instance id: {} & volume id: {}", instanceId, volumeId);
        return AttachVolumeRequest.builder().instanceId(instanceId)
                .volumeId(volumeId).device(deviceName).build();
    }

    private void checkVolumeAttachmentStatus(List<String> instanceIds, AmazonEc2Client client,
            Set<String> volumeIdsToAttach, Throwable throwable) {
        DescribeVolumesResponse describeVolumesResponse = client.describeVolumes(DescribeVolumesRequest.builder()
                .filters(Filter.builder()
                        .name("attachment.instance-id")
                        .values(instanceIds)
                        .build())
                .build());
        LOGGER.info("Describe volume result :{}", describeVolumesResponse);
        List<Volume> volumes = describeVolumesResponse.volumes();
        List<String> volumeIdsForInstance = volumes.stream().map(Volume::volumeId).toList();
        LOGGER.info("Volume IDs to attach {}", volumeIdsToAttach);
        if (!new HashSet<>(volumeIdsForInstance).containsAll(volumeIdsToAttach)) {
            String errorMessage = "Some Volume attachment were unsuccessful. ";
            LOGGER.error(errorMessage);
            throw new CloudbreakServiceException(errorMessage + throwable.getMessage(), throwable);
        }
    }
}
