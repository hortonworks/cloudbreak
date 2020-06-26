package com.sequenceiq.cloudbreak.cloud.aws.encryption;

import static com.sequenceiq.cloudbreak.cloud.aws.encryption.EncryptedImageCopyService.SNAPSHOT_NOT_FOUND_MSG_CODE;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AvailabilityZone;
import com.amazonaws.services.ec2.model.CreateSnapshotRequest;
import com.amazonaws.services.ec2.model.CreateSnapshotResult;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.CreateVolumeRequest;
import com.amazonaws.services.ec2.model.CreateVolumeResult;
import com.amazonaws.services.ec2.model.DeleteSnapshotRequest;
import com.amazonaws.services.ec2.model.DeleteVolumeRequest;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesRequest;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TagSpecification;
import com.sequenceiq.cloudbreak.cloud.aws.AwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.AwsTaggingService;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.AwsBackoffSyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.aws.task.AwsPollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsInstanceView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource.Builder;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class EncryptedSnapshotService {

    static final String VOLUME_NOT_FOUND_MSG_CODE = "InvalidVolume.NotFound";

    private static final Logger LOGGER = LoggerFactory.getLogger(EncryptedSnapshotService.class);

    private static final String CLOUDBREAK_EBS_ENCRYPTION = "cloudbreak-ebs-encryption-%s";

    private static final int VOLUME_SIZE = 10;

    private static final String REGION_NAME = "region-name";

    private static final String ENCRYPTED_SNAPSHOT = "Encrypted snapshot";

    @Inject
    private AwsClient awsClient;

    @Inject
    private AwsPollTaskFactory awsPollTaskFactory;

    @Inject
    private AwsBackoffSyncPollingScheduler<Boolean> awsBackoffSyncPollingScheduler;

    @Inject
    private AwsTaggingService awsTaggingService;

    public boolean isEncryptedVolumeRequested(Group group) {
        return new AwsInstanceView(group.getReferenceInstanceConfiguration().getTemplate()).isEncryptedVolumes();
    }

    public Optional<String> createSnapshotIfNeeded(AuthenticatedContext ac, CloudStack cloudStack, Group group, PersistenceNotifier resourceNotifier) {
        InstanceTemplate instanceTemplate = group.getReferenceInstanceConfiguration().getTemplate();
        String regionName = ac.getCloudContext().getLocation().getRegion().value();
        AwsInstanceView awsInstanceView = new AwsInstanceView(instanceTemplate);
        AwsCredentialView awsCredentialView = new AwsCredentialView(ac.getCloudCredential());

        AmazonEC2Client client = awsClient.createAccess(awsCredentialView, regionName);

        Optional<String> snapshotId = prepareSnapshotForEncryptionBecauseThatDoesNotExist(ac, cloudStack, awsInstanceView, client, resourceNotifier);
        if (snapshotId.isPresent())  {
            saveEncryptedResource(ac, resourceNotifier, ResourceType.AWS_SNAPSHOT, snapshotId.get(), group.getName());
            return snapshotId;
        }
        return snapshotId;
    }

    public void deleteResources(AuthenticatedContext ac, AmazonEC2Client client, List<CloudResource> resources) {
        LOGGER.debug("Deleting attached EBS volume encryption related resources: {}", ac.getCloudContext().getId());
        if (resources != null && !resources.isEmpty()) {
            deleteSnapshotResources(client, resources);
            deleteVolumeResources(client, resources);
        }
    }

    private Optional<String> prepareSnapshotForEncryptionBecauseThatDoesNotExist(AuthenticatedContext ac, CloudStack cloudStack, AwsInstanceView instanceView,
            AmazonEC2Client client, PersistenceNotifier resourceNotifier) {
        LOGGER.debug("Create an encrypted EBS volume for group: '{}'", instanceView.getGroupName());
        CreateVolumeResult volumeResult = client.createVolume(prepareCreateVolumeRequest(ac, instanceView, client, cloudStack));
        String volumeId = volumeResult.getVolume().getVolumeId();
        checkEbsVolumeStatus(ac, client, volumeId);
        saveEncryptedResource(ac, resourceNotifier, ResourceType.AWS_ENCRYPTED_VOLUME, volumeId, instanceView.getGroupName());
        LOGGER.debug("Encrypted EBS volume has been created with id: '{}', for group: '{}'", volumeId, instanceView.getGroupName());

        LOGGER.debug("Create an encrypted snapshot of EBS volume for group: '{}'", instanceView.getGroupName());
        CreateSnapshotResult snapshotResult = client.createSnapshot(prepareCreateSnapshotRequest(volumeResult));
        checkSnapshotReadiness(ac, client, snapshotResult);
        LOGGER.debug("Encrypted snapshot of EBS volume has been created with id: '{}', for group: '{}'", snapshotResult.getSnapshot().getSnapshotId(),
                instanceView.getGroupName());
        client.createTags(prepareCreateTagsRequest(ac, cloudStack, instanceView, snapshotResult));
        return Optional.of(snapshotResult.getSnapshot().getSnapshotId());
    }

    private void saveEncryptedResource(AuthenticatedContext ac, PersistenceNotifier resourceNotifier, ResourceType awsEncryptedVolume,
            String volumeId, String groupName) {
        CloudResource cloudResource = new Builder()
                .type(awsEncryptedVolume)
                .name(volumeId)
                .group(groupName)
                .build();
        resourceNotifier.notifyAllocation(cloudResource, ac.getCloudContext());
    }

    private CreateTagsRequest prepareCreateTagsRequest(AuthenticatedContext ac, CloudStack cloudStack, AwsInstanceView awsInstanceView,
            CreateSnapshotResult snapshotResult) {
        return new CreateTagsRequest().withTags(prepareTagList(ac, cloudStack, awsInstanceView)).withResources(snapshotResult.getSnapshot().getSnapshotId());
    }

    private Collection<Tag> prepareTagList(AuthenticatedContext ac, CloudStack cloudStack, AwsInstanceView awsInstanceView) {
        String ebsEncryptedTag = getEncryptedSnapshotName(awsInstanceView);
        Collection<com.amazonaws.services.ec2.model.Tag> tags = awsTaggingService.prepareEc2Tags(ac, cloudStack.getTags());
        tags.add(new Tag().withKey(ebsEncryptedTag).withValue(ebsEncryptedTag));
        return tags;
    }

    private String getEncryptedSnapshotName(AwsInstanceView awsInstanceView) {
        return String.format(CLOUDBREAK_EBS_ENCRYPTION, awsInstanceView.getTemplateId());
    }

    private void checkSnapshotReadiness(AuthenticatedContext ac, AmazonEC2Client client, CreateSnapshotResult snapshotResult) {
        PollTask<Boolean> snapshotReadyChecker = awsPollTaskFactory
                .newCreateSnapshotReadyStatusCheckerTask(ac, snapshotResult.getSnapshot().getSnapshotId(), client);
        try {
            Boolean statePollerResult = snapshotReadyChecker.call();
            if (!snapshotReadyChecker.completed(statePollerResult)) {
                awsBackoffSyncPollingScheduler.schedule(snapshotReadyChecker);
            }
        } catch (Exception e) {
            throw new CloudConnectorException(e.getMessage(), e);
        }
    }

    private void checkEbsVolumeStatus(AuthenticatedContext ac, AmazonEC2Client client, String volumeId) {
        PollTask<Boolean> ebsVolumeStateChecker = awsPollTaskFactory.newEbsVolumeStatusCheckerTask(ac, client, volumeId);
        try {
            Boolean statePollerResult = ebsVolumeStateChecker.call();
            if (!ebsVolumeStateChecker.completed(statePollerResult)) {
                awsBackoffSyncPollingScheduler.schedule(ebsVolumeStateChecker);
            }
        } catch (Exception e) {
            throw new CloudConnectorException(e.getMessage(), e);
        }
    }

    private CreateSnapshotRequest prepareCreateSnapshotRequest(CreateVolumeResult volumeResult) {
        return new CreateSnapshotRequest().withVolumeId(volumeResult.getVolume().getVolumeId()).withDescription(ENCRYPTED_SNAPSHOT);
    }

    private CreateVolumeRequest prepareCreateVolumeRequest(AuthenticatedContext ac, AwsInstanceView awsInstanceView, AmazonEC2 client, CloudStack cloudStack) {
        String availabilityZone = prepareDescribeAvailabilityZonesResult(ac, client);

        TagSpecification tagSpecification = new TagSpecification()
                .withResourceType(com.amazonaws.services.ec2.model.ResourceType.Volume)
                .withTags(prepareTagList(ac, cloudStack, awsInstanceView));

        CreateVolumeRequest createVolumeRequest = new CreateVolumeRequest()
                .withSize(VOLUME_SIZE)
                .withAvailabilityZone(availabilityZone)
                .withTagSpecifications(tagSpecification)
                .withEncrypted(true);
        if (awsInstanceView.isKmsEnabled()) {
            createVolumeRequest = createVolumeRequest.withKmsKeyId(awsInstanceView.getKmsKey());
        }
        return createVolumeRequest;
    }

    private String prepareDescribeAvailabilityZonesResult(AuthenticatedContext ac, AmazonEC2 client) {
        String regionName = ac.getCloudContext().getLocation().getRegion().value();
        Optional<AvailabilityZone> first =
                client.describeAvailabilityZones(prepareDescribeAvailabilityZoneRequest(regionName)).getAvailabilityZones().stream().findFirst();
        return first.map(AvailabilityZone::getZoneName).orElse(null);
    }

    private DescribeAvailabilityZonesRequest prepareDescribeAvailabilityZoneRequest(String regionName) {
        return new DescribeAvailabilityZonesRequest().withFilters(new Filter().withName(REGION_NAME).withValues(regionName));
    }

    private void deleteSnapshotResources(AmazonEC2Client client, List<CloudResource> resources) {
        Set<CloudResource> encryptedSnapshots = filterResourcesByType(resources, ResourceType.AWS_SNAPSHOT);
        for (CloudResource snapshot : encryptedSnapshots) {
            try {
                DeleteSnapshotRequest deleteSnapshotRequest = new DeleteSnapshotRequest().withSnapshotId(snapshot.getName());
                client.deleteSnapshot(deleteSnapshotRequest);
            } catch (Exception e) {
                String errorMessage = String.format("Failed to delete snapshot [id:'%s'], detailed message: %s", snapshot.getName(), e.getMessage());
                LOGGER.warn(errorMessage, e);
                if (!e.getMessage().contains(SNAPSHOT_NOT_FOUND_MSG_CODE)) {
                    throw new CloudConnectorException(errorMessage, e);
                }
            }
        }
    }

    private void deleteVolumeResources(AmazonEC2Client client, List<CloudResource> resources) {
        Set<CloudResource> encryptedVolumes = filterResourcesByType(resources, ResourceType.AWS_ENCRYPTED_VOLUME);
        for (CloudResource volume : encryptedVolumes) {
            try {
                DeleteVolumeRequest deleteVolumeRequest = new DeleteVolumeRequest().withVolumeId(volume.getName());
                client.deleteVolume(deleteVolumeRequest);
            } catch (Exception e) {
                String errorMessage = String.format("Failed to delete volume [id:'%s'], detailed message: %s", volume.getName(), e.getMessage());
                LOGGER.warn(errorMessage, e);
                if (!e.getMessage().contains(VOLUME_NOT_FOUND_MSG_CODE)) {
                    throw new CloudConnectorException(errorMessage, e);
                }
            }
        }
    }

    private Set<CloudResource> filterResourcesByType(List<CloudResource> resources, ResourceType resourceType) {
        return resources.stream()
                .filter(cloudResource -> cloudResource.getType().equals(resourceType))
                .collect(Collectors.toSet());
    }
}
