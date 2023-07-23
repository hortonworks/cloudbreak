package com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource;

import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.dyngr.Polling;
import com.dyngr.core.AttemptResults;
import com.sequenceiq.cloudbreak.cloud.ResourceVolumeConnector;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AuthenticatedContextView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes.Volume;

import software.amazon.awssdk.services.ec2.model.DeleteVolumeRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.DetachVolumeRequest;
import software.amazon.awssdk.services.ec2.model.VolumeState;

@Service
public class AwsResourceVolumeConnector implements ResourceVolumeConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsResourceVolumeConnector.class);

    private static final int MAX_READ_COUNT = 15;

    private static final int SLEEP_INTERVAL = 2;

    @Override
    public void detachVolumes(AuthenticatedContext authenticatedContext, List<CloudResource> cloudResources) throws Exception {
        Map<String, List<Volume>> instanceVolumeIdsMap = cloudResources.stream().filter(res -> null != res.getInstanceId())
                .collect(toMap(CloudResource::getInstanceId, resource -> resource.getParameter(CloudResource.ATTRIBUTES,
                        VolumeSetAttributes.class).getVolumes()));
        AmazonEc2Client amazonEC2Client = getEc2Client(authenticatedContext);
        List<String> volumeIdsToPoll = new ArrayList<>();
        for (Entry<String, List<Volume>> entry: instanceVolumeIdsMap.entrySet()) {
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

    @Override
    public void deleteVolumes(AuthenticatedContext authenticatedContext, List<CloudResource> cloudResources) throws Exception {
        Map<String, List<Volume>> instanceVolumeIdsMap = cloudResources.stream().filter(res -> null != res.getInstanceId())
                .collect(toMap(CloudResource::getInstanceId, resource -> resource.getParameter(CloudResource.ATTRIBUTES,
                        VolumeSetAttributes.class).getVolumes()));
        AmazonEc2Client amazonEC2Client = getEc2Client(authenticatedContext);
        for (Entry<String, List<Volume>> entry: instanceVolumeIdsMap.entrySet()) {
            entry.getValue().forEach(volume -> {
                LOGGER.debug("Deleting volume after detach - {}", volume.getId());
                DeleteVolumeRequest deleteVolumeRequest = DeleteVolumeRequest.builder().volumeId(volume.getId()).build();
                amazonEC2Client.deleteVolume(deleteVolumeRequest);
            });
        }
    }

    protected AmazonEc2Client getEc2Client(AuthenticatedContext authenticatedContext) {
        return new AuthenticatedContextView(authenticatedContext).getAmazonEC2Client();
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
        Polling.waitPeriodly(SLEEP_INTERVAL, TimeUnit.SECONDS).stopIfException(true).stopAfterAttempt(MAX_READ_COUNT)
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
