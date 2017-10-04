package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateSnapshotRequest;
import com.amazonaws.services.ec2.model.CreateSnapshotResult;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.CreateVolumeRequest;
import com.amazonaws.services.ec2.model.CreateVolumeResult;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesRequest;
import com.amazonaws.services.ec2.model.DescribeSnapshotsRequest;
import com.amazonaws.services.ec2.model.DescribeSnapshotsResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Tag;
import com.google.common.collect.ImmutableList;
import com.sequenceiq.cloudbreak.cloud.aws.task.AwsPollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsInstanceView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;

@Service
public class EncryptedSnapshotPreparator {

    private static final String CLOUDBREAK_EBS_SNAPSHOT = "cloudbreak-ebs-snapshot-%s";

    private static final int VOLUME_SIZE = 10;

    private static final String REGION_NAME = "region-name";

    private static final String TAG_KEY = "tag-key";

    private static final String ENCRYPTED_SNAPSHOT = "Encrypted snapshot";

    @Inject
    private AwsClient awsClient;

    @Inject
    private AwsPollTaskFactory awsPollTaskFactory;

    @Inject
    private SyncPollingScheduler<Boolean> syncPollingScheduler;

    public Optional<String> createSnapshotIfNeeded(AuthenticatedContext ac, Group group) {
        InstanceTemplate instanceTemplate = group.getInstances().get(0).getTemplate();
        String regionName = ac.getCloudContext().getLocation().getRegion().value();
        AwsInstanceView awsInstanceView = new AwsInstanceView(instanceTemplate);
        AwsCredentialView awsCredentialView = new AwsCredentialView(ac.getCloudCredential());

        AmazonEC2Client client = awsClient.createAccess(awsCredentialView, regionName);

        Optional<String> snapshotId = checkThatSnapshotIsAvailable(awsInstanceView, client);
        if (snapshotId.isPresent()) {
            return snapshotId;
        } else {
            return prepareSnapshotForEncryptionBecauseThatDoesNotExist(ac, group, awsInstanceView, client);
        }
    }

    private Optional<String> prepareSnapshotForEncryptionBecauseThatDoesNotExist(AuthenticatedContext ac, Group group,
            AwsInstanceView instanceView, AmazonEC2Client client) {
        CreateVolumeResult volumeResult = client.createVolume(prepareCreateVolumeRequest(ac, instanceView, client));

        checkEbsVolumeStatus(ac, group, client, volumeResult);
        CreateSnapshotResult snapshotResult = client.createSnapshot(prepareCreateSnapshotRequest(volumeResult));

        checkSnapshotReadiness(ac, client, snapshotResult);
        client.createTags(prepareCreateTagsRequest(instanceView, snapshotResult));
        return Optional.of(snapshotResult.getSnapshot().getSnapshotId());
    }

    private Optional<String> checkThatSnapshotIsAvailable(AwsInstanceView awsInstanceView, AmazonEC2Client client) {
        DescribeSnapshotsResult describeSnapshotsResult = client.describeSnapshots(prepareDescribeSnapshotsRequest(awsInstanceView));
        if (describeSnapshotsResult.getSnapshots().isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(describeSnapshotsResult.getSnapshots().get(0).getSnapshotId());
    }

    private CreateTagsRequest prepareCreateTagsRequest(AwsInstanceView awsInstanceView, CreateSnapshotResult snapshotResult) {
        return new CreateTagsRequest().withTags(prepareTagList(awsInstanceView)).withResources(snapshotResult.getSnapshot().getSnapshotId());
    }

    private ImmutableList<Tag> prepareTagList(AwsInstanceView awsInstanceView) {
        return ImmutableList.of(new Tag().withKey(CLOUDBREAK_EBS_SNAPSHOT).withValue(String.format(CLOUDBREAK_EBS_SNAPSHOT, awsInstanceView.getTemplateId())));
    }

    private void checkSnapshotReadiness(AuthenticatedContext ac, AmazonEC2Client client, CreateSnapshotResult snapshotResult) {
        PollTask<Boolean> snapshotReadyChecker = awsPollTaskFactory
                .newCreateSnapshotReadyStatusCheckerTask(ac, snapshotResult.getSnapshot().getSnapshotId(), client);
        try {
            Boolean statePollerResult = snapshotReadyChecker.call();
            if (!snapshotReadyChecker.completed(statePollerResult)) {
                syncPollingScheduler.schedule(snapshotReadyChecker);
            }
        } catch (Exception e) {
            throw new CloudConnectorException(e.getMessage(), e);
        }
    }

    private void checkEbsVolumeStatus(AuthenticatedContext ac, Group group, AmazonEC2Client client, CreateVolumeResult volumeResult) {
        PollTask<Boolean> ebsVolumeStateChecker = awsPollTaskFactory.newEbsVolumeStatusCheckerTask(ac, group, client, volumeResult.getVolume().getVolumeId());
        try {
            Boolean statePollerResult = ebsVolumeStateChecker.call();
            if (!ebsVolumeStateChecker.completed(statePollerResult)) {
                syncPollingScheduler.schedule(ebsVolumeStateChecker);
            }
        } catch (Exception e) {
            throw new CloudConnectorException(e.getMessage(), e);
        }
    }

    private CreateSnapshotRequest prepareCreateSnapshotRequest(CreateVolumeResult volumeResult) {
        return new CreateSnapshotRequest().withVolumeId(volumeResult.getVolume().getVolumeId()).withDescription(ENCRYPTED_SNAPSHOT);
    }

    private CreateVolumeRequest prepareCreateVolumeRequest(AuthenticatedContext ac, AwsInstanceView awsInstanceView, AmazonEC2Client client) {
        String availabilityZone = prepareDescribeAvailabilityZonesResult(ac, client);

        CreateVolumeRequest createVolumeRequest = new CreateVolumeRequest().withSize(VOLUME_SIZE).withAvailabilityZone(availabilityZone).withEncrypted(true);
        if (awsInstanceView.isKmsEnabled()) {
            createVolumeRequest.withKmsKeyId(awsInstanceView.getKmsKey());
        }
        return createVolumeRequest;
    }

    private String prepareDescribeAvailabilityZonesResult(AuthenticatedContext ac, AmazonEC2Client client) {
        String regionName = ac.getCloudContext().getLocation().getRegion().value();
        return client.describeAvailabilityZones(prepareDescribeAvailabilityZoneRequest(regionName)).getAvailabilityZones().get(0).getZoneName();
    }

    private DescribeAvailabilityZonesRequest prepareDescribeAvailabilityZoneRequest(String regionName) {
        return new DescribeAvailabilityZonesRequest().withFilters(new Filter().withName(REGION_NAME).withValues(regionName));
    }

    private DescribeSnapshotsRequest prepareDescribeSnapshotsRequest(AwsInstanceView awsInstanceView) {
        return new DescribeSnapshotsRequest().withFilters(prepareFilters(awsInstanceView));
    }

    private Filter prepareFilters(AwsInstanceView awsInstanceView) {
        return new Filter().withName(TAG_KEY).withValues(String.format(CLOUDBREAK_EBS_SNAPSHOT, awsInstanceView.getTemplateId()));
    }

    public boolean isEncryptedVolumeRequested(Group group) {
        return group.getInstances().stream().anyMatch(cloudInstance -> new AwsInstanceView(cloudInstance.getTemplate()).isEncryptedVolumes());
    }

}
