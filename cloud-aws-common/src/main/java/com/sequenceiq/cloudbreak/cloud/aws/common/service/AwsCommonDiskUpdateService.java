package com.sequenceiq.cloudbreak.cloud.aws.common.service;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.dyngr.Polling;
import com.dyngr.core.AttemptResults;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AuthenticatedContextView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

import software.amazon.awssdk.services.ec2.model.DescribeVolumesModificationsRequest;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.ModifyVolumeRequest;
import software.amazon.awssdk.services.ec2.model.VolumeModification;
import software.amazon.awssdk.services.ec2.model.VolumeModificationState;

@Service
public class AwsCommonDiskUpdateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsCommonDiskUpdateService.class);

    private static final int MAX_READ_COUNT = 20;

    private static final int SLEEP_INTERVAL = 30;

    public void modifyVolumes(AuthenticatedContext authenticatedContext, List<String> volumeIds, String diskType, int size) throws Exception {
        AmazonEc2Client amazonEC2Client = getEc2Client(authenticatedContext);
        for (String volume: volumeIds) {
            LOGGER.debug("Modifying volume {} to disk type {} and size {}", volume, diskType, size);
            try {
                ModifyVolumeRequest modifyVolumeRequest = ModifyVolumeRequest.builder().volumeId(volume).volumeType(diskType).size(size).build();
                amazonEC2Client.modifyVolume(modifyVolumeRequest);
            } catch (Ec2Exception ex) {
                LOGGER.error(String.format("AWS threw BAD Request exception: %s", ex.getMessage()));
                throw new CloudbreakException(String.format("Exception while modifying disk volumes: %s", ex.getMessage()));
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
    }

    protected AmazonEc2Client getEc2Client(AuthenticatedContext authenticatedContext) {
        return new AuthenticatedContextView(authenticatedContext).getAmazonEC2Client();
    }

    protected List<VolumeModification> getVolumeModificationsState(List<String> volumeIds, AmazonEc2Client amazonEC2Client) {
        LOGGER.debug("Checking modification status for Volume {}", volumeIds);
        DescribeVolumesModificationsRequest describeVolumesModificationsRequest = DescribeVolumesModificationsRequest.builder()
                .volumeIds(volumeIds).build();
        return amazonEC2Client.describeVolumeModification(describeVolumesModificationsRequest).volumesModifications();
    }
}
