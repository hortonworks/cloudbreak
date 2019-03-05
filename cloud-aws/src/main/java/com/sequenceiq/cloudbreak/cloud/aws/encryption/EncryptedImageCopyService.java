package com.sequenceiq.cloudbreak.cloud.aws.encryption;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNoneEmpty;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.BlockDeviceMapping;
import com.amazonaws.services.ec2.model.CopyImageRequest;
import com.amazonaws.services.ec2.model.CopyImageResult;
import com.amazonaws.services.ec2.model.DeleteSnapshotRequest;
import com.amazonaws.services.ec2.model.DeregisterImageRequest;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.Image;
import com.sequenceiq.cloudbreak.cloud.aws.AwsClient;
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
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

@Service
public class EncryptedImageCopyService {

    static final String SNAPSHOT_NOT_FOUND_MSG_CODE = "InvalidSnapshot.NotFound";

    static final String AMI_NOT_FOUND_MSG_CODE = "InvalidAMIID.NotFound";

    private static final String ENCRYPTED_AMI_NAME_PATTERN = "encrypted-cb-%s-%s";

    private static final String ENCRYPTED_AMI_DESCRIPTION = "Encrypted AMI for Cloudbreak root volume in an encrypted hostgroup.";

    private static final Logger LOGGER = LoggerFactory.getLogger(EncryptedImageCopyService.class);

    @Inject
    private AwsClient awsClient;

    @Inject
    private AwsPollTaskFactory awsPollTaskFactory;

    @Inject
    private SyncPollingScheduler<Boolean> syncPollingScheduler;

    public Map<String, String> createEncryptedImages(AuthenticatedContext ac, CloudStack cloudStack, PersistenceNotifier resourceNotifier) {
        String selectedAMIName = cloudStack.getImage().getImageName();
        AwsCredentialView awsCredentialView = new AwsCredentialView(ac.getCloudCredential());
        String regionName = ac.getCloudContext().getLocation().getRegion().value();
        AmazonEC2Client client = awsClient.createAccess(awsCredentialView, regionName);

        Map<String, EncryptedImageConfig> configByGroupName = getEncryptedImageConfigByGroup(cloudStack, selectedAMIName, ac, client, resourceNotifier);

        Map<String, String> imageIdByGroupName = configByGroupName.entrySet()
                .stream()
                .collect(Collectors.toMap(Entry::getKey, e -> e.getValue().getImageId()));
        if (!imageIdByGroupName.isEmpty()) {
            Collection<String> imageIds = new HashSet<>(imageIdByGroupName.values());
            LOGGER.debug("Start polling the availability of the created AMIs: '{}'", String.join(",", imageIds));
            checkBooleanPollTask(awsPollTaskFactory.newAMICopyStatusCheckerTask(ac, imageIds, client));
        }
        return imageIdByGroupName;
    }

    public void deleteResources(String regionName, AmazonEC2Client client, List<CloudResource> resources) {
        if (resources != null && !resources.isEmpty()) {
            deleteImageAndItsSnapshotResources(client, resources, regionName);
        }
    }

    private Map<String, EncryptedImageConfig> getEncryptedImageConfigByGroup(CloudStack cloudStack, String selectedAMIName, AuthenticatedContext ac,
            AmazonEC2Client client, PersistenceNotifier resourceNotifier) {

        Map<String, EncryptedImageConfig> configByGroupName = new HashMap<>();
        for (Group group : cloudStack.getGroups()) {
            if (isEncryptedVolumeRequested(group)) {
                InstanceTemplate instanceTemplate = group.getReferenceInstanceConfiguration().getTemplate();
                AwsInstanceView awsInstanceView = new AwsInstanceView(instanceTemplate);
                Optional<String> actualKmsKey = Optional.ofNullable(awsInstanceView.getKmsKey());

                Optional<EncryptedImageConfig> existingEncryptedImageConfig = configByGroupName.values()
                        .stream()
                        .filter(encryptedImageConfig -> encryptedImageConfig.getKmsKey().equals(actualKmsKey))
                        .findFirst();

                EncryptedImageConfig encryptedImageConfig = existingEncryptedImageConfig
                        .orElseGet(() -> createEncryptedImageFromAMI(client, awsInstanceView, selectedAMIName, ac, resourceNotifier));
                configByGroupName.put(group.getName(), encryptedImageConfig);
            }
        }
        return configByGroupName;
    }

    private boolean isEncryptedVolumeRequested(Group group) {
        return new AwsInstanceView(group.getReferenceInstanceConfiguration().getTemplate()).isEncryptedVolumes();
    }

    private EncryptedImageConfig createEncryptedImageFromAMI(AmazonEC2Client client, AwsInstanceView awsInstanceView, String selectedAMIName,
            AuthenticatedContext ac, PersistenceNotifier resourceNotifier) {

        String regionName = ac.getCloudContext().getLocation().getRegion().value();
        CopyImageResult copyImageResult = client.copyImage(createCopyImageRequest(selectedAMIName, regionName, awsInstanceView));
        String imageId = copyImageResult.getImageId();
        CloudResource cloudResource = new Builder()
                .type(ResourceType.AWS_ENCRYPTED_AMI)
                .name(imageId)
                .group(awsInstanceView.getGroupName())
                .build();

        resourceNotifier.notifyAllocation(cloudResource, ac.getCloudContext());
        LOGGER.debug("The source AMI '{}' has been copied and encrypted with id: '{}' in region: '{}'", selectedAMIName, imageId, regionName);
        return new EncryptedImageConfig(imageId, Optional.ofNullable(awsInstanceView.getKmsKey()));
    }

    private CopyImageRequest createCopyImageRequest(String selectedAMIName, String regionName, AwsInstanceView awsInstanceView) {
        CopyImageRequest copyImageRequest = new CopyImageRequest()
                .withEncrypted(true)
                .withSourceRegion(regionName)
                .withSourceImageId(selectedAMIName)
                .withName(format(ENCRYPTED_AMI_NAME_PATTERN, selectedAMIName, awsInstanceView.getTemplateId()))
                .withDescription(ENCRYPTED_AMI_DESCRIPTION);
        if (awsInstanceView.isKmsCustom()) {
            copyImageRequest = copyImageRequest.withKmsKeyId(awsInstanceView.getKmsKey());
        }
        return copyImageRequest;
    }

    private void checkBooleanPollTask(PollTask<Boolean> booleanPollTask) {
        try {
            Boolean statePollerResult = booleanPollTask.call();
            if (!booleanPollTask.completed(statePollerResult)) {
                syncPollingScheduler.schedule(booleanPollTask);
            }
        } catch (Exception e) {
            throw new CloudConnectorException(e.getMessage(), e);
        }
    }

    private void deleteImageAndItsSnapshotResources(AmazonEC2Client client, List<CloudResource> resources, String regionName) {
        Set<CloudResource> encryptedAMIs = filterEncryptedAMIResources(resources);
        for (CloudResource encryptedImage : encryptedAMIs) {
            try {
                DescribeImagesRequest describeImagesRequest = new DescribeImagesRequest().withImageIds(encryptedImage.getName());
                client.describeImages(describeImagesRequest)
                        .getImages()
                        .stream()
                        .findFirst()
                        .ifPresent(image -> deleteImage(client, encryptedImage, image, regionName));
            } catch (Exception e) {
                String errorMessage = format("Failed to delete image(AMI) [id:'%s'], in region: '%s', detailed message: %s",
                        encryptedImage.getName(), regionName, e.getMessage());
                LOGGER.warn(errorMessage, e);
                if (!e.getMessage().contains(AMI_NOT_FOUND_MSG_CODE) && !e.getMessage().contains(SNAPSHOT_NOT_FOUND_MSG_CODE)) {
                    throw new CloudConnectorException(errorMessage, e);
                }
            }
        }
    }

    private Set<CloudResource> filterEncryptedAMIResources(List<CloudResource> resources) {
        return resources.stream()
                .filter(cloudResource -> cloudResource.getType().equals(ResourceType.AWS_ENCRYPTED_AMI))
                .collect(Collectors.toSet());
    }

    private void deleteImage(AmazonEC2Client client, CloudResource encryptedImage, Image imageOptional, String regionName) {
        LOGGER.debug("Deregister encrypted AMI: '{}', in region: '{}'", encryptedImage.getName(), regionName);
        DeregisterImageRequest deregisterImageRequest = new DeregisterImageRequest().withImageId(encryptedImage.getName());
        client.deregisterImage(deregisterImageRequest);

        imageOptional.getBlockDeviceMappings()
                .stream()
                .filter(deviceMapping -> deviceMapping.getEbs() != null && isNoneEmpty(deviceMapping.getEbs().getSnapshotId()))
                .forEach(deviceMapping -> deleteSnapshot(client, deviceMapping, encryptedImage, regionName));
    }

    private void deleteSnapshot(AmazonEC2Client client, BlockDeviceMapping blockDeviceMapping, CloudResource encryptedImage, String regionName) {
        String snapshotId = blockDeviceMapping.getEbs().getSnapshotId();
        String imageName = encryptedImage.getName();
        try {
            LOGGER.debug("Delete encrypted AMI's ['{}'] snapshot ['{}'], in region: '{}'", imageName, snapshotId, regionName);
            DeleteSnapshotRequest deleteSnapshotRequest = new DeleteSnapshotRequest().withSnapshotId(snapshotId);
            client.deleteSnapshot(deleteSnapshotRequest);
        } catch (Exception e) {
            String errorMessage = format("Failed to delete snapshot [id:'%s'] of AMI [id:'%s'], in region: '%s', detailed message: %s",
                    snapshotId, imageName, regionName, e.getMessage());
            LOGGER.warn(errorMessage, e);
            throw new CloudConnectorException(errorMessage, e);
        }
    }

    private static class EncryptedImageConfig {

        private final String imageId;

        private final Optional<String> kmsKey;

        EncryptedImageConfig(String imageId, Optional<String> kmsKey) {
            this.imageId = imageId;
            this.kmsKey = kmsKey;
        }

        String getImageId() {
            return imageId;
        }

        Optional<String> getKmsKey() {
            return kmsKey;
        }
    }
}
