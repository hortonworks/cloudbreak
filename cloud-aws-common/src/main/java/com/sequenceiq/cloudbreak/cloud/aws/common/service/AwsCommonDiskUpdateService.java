package com.sequenceiq.cloudbreak.cloud.aws.common.service;

import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.dyngr.Polling;
import com.dyngr.core.AttemptResults;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AuthenticatedContextView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

import software.amazon.awssdk.services.ec2.model.DeleteVolumeRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesModificationsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.DetachVolumeRequest;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.ModifyVolumeRequest;
import software.amazon.awssdk.services.ec2.model.VolumeModification;
import software.amazon.awssdk.services.ec2.model.VolumeModificationState;
import software.amazon.awssdk.services.ec2.model.VolumeState;

@Service
public class AwsCommonDiskUpdateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsCommonDiskUpdateService.class);

    private static final int MAX_READ_COUNT = 20;

    private static final int SLEEP_INTERVAL = 30;

    private static final int MAX_READ_COUNT_DELETE = 15;

    private static final int SLEEP_INTERVAL_DELETE = 2;

    public void modifyVolumes(AuthenticatedContext authenticatedContext, List<String> volumeIds, String diskType, int size) throws Exception {
        AmazonEc2Client amazonEC2Client = getEc2Client(authenticatedContext);
        for (String volume: volumeIds) {
            LOGGER.debug("Modifying volume {} to disk type {} and size {}", volume, diskType, size);
            try {
                ModifyVolumeRequest modifyVolumeRequest = ModifyVolumeRequest.builder().volumeId(volume).volumeType(diskType).size(size).build();
                amazonEC2Client.modifyVolume(modifyVolumeRequest);
            } catch (Ec2Exception ex) {
                LOGGER.error("AWS threw BAD Request exception, while modifying volume: {}, exception: {}", volume, ex.getMessage());
                throw new CloudbreakException(String.format("Exception while modifying disk volume: %s, exception: %s", volume, ex.getMessage()));
            }
        }
        Polling.waitPeriodly(SLEEP_INTERVAL, TimeUnit.SECONDS).stopIfException(false).stopAfterAttempt(MAX_READ_COUNT)
            .run(() -> {
                LOGGER.debug("Getting volume modification states - {}", volumeIds);
                boolean volumeStateIsNotComplete = getVolumeModificationsState(volumeIds, amazonEC2Client).stream()
                        .map(VolumeModification::modificationState)
                        .anyMatch(state -> !state.equals(VolumeModificationState.COMPLETED) && !state.equals(VolumeModificationState.FAILED)
                                && !state.equals(VolumeModificationState.OPTIMIZING));
                LOGGER.debug("Result of checking all volumes available - {}", volumeStateIsNotComplete);
                if (volumeStateIsNotComplete) {
                    return AttemptResults.justContinue();
                }
                return AttemptResults.justFinish();
            });
        List<String> failedVolumes = getVolumeModificationsState(volumeIds, amazonEC2Client).stream()
                .filter(volMod -> volMod.modificationState().equals(VolumeModificationState.FAILED)).map(VolumeModification::volumeId).toList();
        if (!CollectionUtils.isEmpty(failedVolumes)) {
            throw new CloudbreakException(String.format("Some volumes were not modified: %s, please retry after 6 hours",
                    String.join("'", failedVolumes)));
        }
    }

    protected AmazonEc2Client getEc2Client(AuthenticatedContext authenticatedContext) {
        return new AuthenticatedContextView(authenticatedContext).getAmazonEC2Client();
    }

    protected List<VolumeModification> getVolumeModificationsState(List<String> volumeIds, AmazonEc2Client amazonEC2Client) {
        LOGGER.debug("Checking modification status for Volumes {}", volumeIds);
        DescribeVolumesModificationsRequest describeVolumesModificationsRequest = DescribeVolumesModificationsRequest.builder()
                .volumeIds(volumeIds).build();
        return amazonEC2Client.describeVolumeModifications(describeVolumesModificationsRequest).volumesModifications();
    }

    public void detachVolumes(AuthenticatedContext authenticatedContext, List<CloudResource> cloudResources) throws Exception {
        Map<String, List<VolumeSetAttributes.Volume>> instanceVolumeIdsMap = cloudResources.stream().filter(res -> null != res.getInstanceId())
                .collect(toMap(CloudResource::getInstanceId, resource -> resource.getParameter(CloudResource.ATTRIBUTES,
                        VolumeSetAttributes.class).getVolumes()));
        AmazonEc2Client amazonEC2Client = getEc2Client(authenticatedContext);
        List<String> volumeIdsToPoll = new ArrayList<>();
        for (Map.Entry<String, List<VolumeSetAttributes.Volume>> entry: instanceVolumeIdsMap.entrySet()) {
            entry.getValue().forEach(volume -> {
                if (VolumeState.IN_USE.equals(getVolumeStates(List.of(volume.getId()), amazonEC2Client).get(0))) {
                    LOGGER.debug("Starting to detach volume - {}", volume.getId());
                    volumeIdsToPoll.add(volume.getId());
                    DetachVolumeRequest detachVolumeRequest = DetachVolumeRequest.builder().instanceId(entry.getKey()).volumeId(volume.getId())
                            .device(volume.getDevice()).build();
                    amazonEC2Client.detachVolume(detachVolumeRequest);
                }
            });
        }
        pollVolumeStates(amazonEC2Client, volumeIdsToPoll);
    }

    public void deleteVolumes(AuthenticatedContext authenticatedContext, List<CloudResource> cloudResources) throws Exception {
        Map<String, List<VolumeSetAttributes.Volume>> instanceVolumeIdsMap = cloudResources.stream().filter(res -> null != res.getInstanceId())
                .collect(toMap(CloudResource::getInstanceId, resource -> resource.getParameter(CloudResource.ATTRIBUTES,
                        VolumeSetAttributes.class).getVolumes()));
        AmazonEc2Client amazonEC2Client = getEc2Client(authenticatedContext);
        for (Map.Entry<String, List<VolumeSetAttributes.Volume>> entry: instanceVolumeIdsMap.entrySet()) {
            entry.getValue().forEach(volume -> {
                LOGGER.debug("Deleting volume after detach - {}", volume.getId());
                DeleteVolumeRequest deleteVolumeRequest = DeleteVolumeRequest.builder().volumeId(volume.getId()).build();
                amazonEC2Client.deleteVolume(deleteVolumeRequest);
            });
        }
    }

    protected List<VolumeState> getVolumeStates(List<String> volumeIds, AmazonEc2Client amazonEC2Client) {
        DescribeVolumesRequest describeVolumesRequest = DescribeVolumesRequest.builder().volumeIds(volumeIds).build();
        DescribeVolumesResponse volumesResponse = amazonEC2Client.describeVolumes(describeVolumesRequest);
        if (volumesResponse.hasVolumes()) {
            return volumesResponse.volumes().stream().map(software.amazon.awssdk.services.ec2.model.Volume::state).collect(Collectors.toList());
        }
        return List.of(VolumeState.UNKNOWN_TO_SDK_VERSION);
    }

    protected void pollVolumeStates(AmazonEc2Client amazonEC2Client, List<String> volumeIdsToPoll) {
        LOGGER.debug("Polling volume states - {}", volumeIdsToPoll);
        Polling.waitPeriodly(SLEEP_INTERVAL_DELETE, TimeUnit.SECONDS).stopIfException(true).stopAfterAttempt(MAX_READ_COUNT_DELETE)
                .run(() -> {
                    LOGGER.debug("Getting volume states - {}", volumeIdsToPoll);
                    boolean volumeStateIsNotAvailable = getVolumeStates(volumeIdsToPoll, amazonEC2Client).stream()
                            .anyMatch(state -> !state.equals(VolumeState.AVAILABLE));
                    LOGGER.debug("Result of checking all volumes available - {}", volumeStateIsNotAvailable);
                    if (volumeStateIsNotAvailable) {
                        return AttemptResults.justContinue();
                    }
                    return AttemptResults.justFinish();
                });
    }
}
