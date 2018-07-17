package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.Collection;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AvailabilityZone;
import com.amazonaws.services.ec2.model.CreateSnapshotRequest;
import com.amazonaws.services.ec2.model.CreateSnapshotResult;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.CreateVolumeRequest;
import com.amazonaws.services.ec2.model.CreateVolumeResult;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesRequest;
import com.amazonaws.services.ec2.model.DescribeSnapshotsRequest;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Tag;
import com.google.common.collect.ImmutableList;
import com.sequenceiq.cloudbreak.cloud.aws.task.AwsPollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsInstanceView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

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

    @Inject
    private AwsTagPreparationService awsTagPreparationService;

    public Optional<String> createSnapshotIfNeeded(AuthenticatedContext ac, CloudStack cloudStack, Group group, PersistenceNotifier resourceNotifier) {
        InstanceTemplate instanceTemplate = group.getReferenceInstanceConfiguration().getTemplate();
        String regionName = ac.getCloudContext().getLocation().getRegion().value();
        AwsInstanceView awsInstanceView = new AwsInstanceView(instanceTemplate);
        AwsCredentialView awsCredentialView = new AwsCredentialView(ac.getCloudCredential());

        AmazonEC2Client client = awsClient.createAccess(awsCredentialView, regionName);

        Optional<String> snapshotId = prepareSnapshotForEncryptionBecauseThatDoesNotExist(ac, cloudStack, awsInstanceView, client);
        if (snapshotId.isPresent())  {
            CloudResource cloudFormationStack = new CloudResource.Builder()
                    .type(ResourceType.AWS_SNAPSHOT)
                    .name(snapshotId.get())
                    .group(group.getName())
                    .build();
            resourceNotifier.notifyAllocation(cloudFormationStack, ac.getCloudContext());
            return snapshotId;
        }
        return snapshotId;
    }

    private Optional<String> prepareSnapshotForEncryptionBecauseThatDoesNotExist(AuthenticatedContext ac, CloudStack cloudStack, AwsInstanceView instanceView,
            AmazonEC2Client client) {
        CreateVolumeResult volumeResult = client.createVolume(prepareCreateVolumeRequest(ac, instanceView, client));

        checkEbsVolumeStatus(ac, client, volumeResult);
        CreateSnapshotResult snapshotResult = client.createSnapshot(prepareCreateSnapshotRequest(volumeResult));

        checkSnapshotReadiness(ac, client, snapshotResult);
        client.createTags(prepareCreateTagsRequest(ac, cloudStack, instanceView, snapshotResult));
        return Optional.of(snapshotResult.getSnapshot().getSnapshotId());
    }

    private CreateTagsRequest prepareCreateTagsRequest(AuthenticatedContext ac, CloudStack cloudStack, AwsInstanceView awsInstanceView,
            CreateSnapshotResult snapshotResult) {
        return new CreateTagsRequest().withTags(prepareTagList(ac, cloudStack, awsInstanceView)).withResources(snapshotResult.getSnapshot().getSnapshotId());
    }

    private Collection<Tag> prepareTagList(AuthenticatedContext ac, CloudStack cloudStack, AwsInstanceView awsInstanceView) {
        Collection<com.amazonaws.services.ec2.model.Tag> tags = awsTagPreparationService.prepareEc2Tags(ac, cloudStack.getTags());
        tags.add(new Tag().withKey(getEncryptedSnapshotName(awsInstanceView)).withValue(getEncryptedSnapshotName(awsInstanceView)));
        return ImmutableList.of(new Tag()
                .withKey(getEncryptedSnapshotName(awsInstanceView))
                .withValue(getEncryptedSnapshotName(awsInstanceView)));
    }

    private String getEncryptedSnapshotName(AwsInstanceView awsInstanceView) {
        return String.format(CLOUDBREAK_EBS_SNAPSHOT, awsInstanceView.getTemplateId());
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

    private void checkEbsVolumeStatus(AuthenticatedContext ac, AmazonEC2Client client, CreateVolumeResult volumeResult) {
        PollTask<Boolean> ebsVolumeStateChecker = awsPollTaskFactory.newEbsVolumeStatusCheckerTask(ac, client, volumeResult.getVolume().getVolumeId());
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

    private CreateVolumeRequest prepareCreateVolumeRequest(AuthenticatedContext ac, AwsInstanceView awsInstanceView, AmazonEC2 client) {
        String availabilityZone = prepareDescribeAvailabilityZonesResult(ac, client);

        CreateVolumeRequest createVolumeRequest = new CreateVolumeRequest().withSize(VOLUME_SIZE).withAvailabilityZone(availabilityZone).withEncrypted(true);
        if (awsInstanceView.isKmsEnabled()) {
            createVolumeRequest.withKmsKeyId(awsInstanceView.getKmsKey());
        }
        return createVolumeRequest;
    }

    private String prepareDescribeAvailabilityZonesResult(AuthenticatedContext ac, AmazonEC2 client) {
        String regionName = ac.getCloudContext().getLocation().getRegion().value();
        Optional<AvailabilityZone> first =
                client.describeAvailabilityZones(prepareDescribeAvailabilityZoneRequest(regionName)).getAvailabilityZones().stream().findFirst();
        return first.isPresent() ? first.get().getZoneName() : null;
    }

    private DescribeAvailabilityZonesRequest prepareDescribeAvailabilityZoneRequest(String regionName) {
        return new DescribeAvailabilityZonesRequest().withFilters(new Filter().withName(REGION_NAME).withValues(regionName));
    }

    private DescribeSnapshotsRequest prepareDescribeSnapshotsRequest(AwsInstanceView awsInstanceView) {
        return new DescribeSnapshotsRequest().withFilters(prepareFilters(awsInstanceView));
    }

    private Filter prepareFilters(AwsInstanceView awsInstanceView) {
        return new Filter().withName(TAG_KEY).withValues(getEncryptedSnapshotName(awsInstanceView));
    }

    public boolean isEncryptedVolumeRequested(Group group) {
        return new AwsInstanceView(group.getReferenceInstanceConfiguration().getTemplate()).isEncryptedVolumes();
    }

}
