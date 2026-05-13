package com.sequenceiq.cloudbreak.service.stackpatch;

import static com.sequenceiq.cloudbreak.domain.stack.StackPatchType.AWS_GP2_TO_GP3_MIGRATION;
import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.resource.volume.AwsVolumeIopsCalculator;
import com.sequenceiq.cloudbreak.cloud.aws.common.service.AwsCommonDiskUpdateService;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AuthenticatedContextView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudConnectResources;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeModificationState;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchType;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stackstatus.StackStatusService;
import com.sequenceiq.cloudbreak.util.CloudConnectorHelper;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.common.model.AwsDiskType;

import software.amazon.awssdk.services.ec2.model.DescribeVolumesModificationsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesModificationsResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.Volume;
import software.amazon.awssdk.services.ec2.model.VolumeModification;
import software.amazon.awssdk.services.ec2.model.VolumeModificationState;
import software.amazon.awssdk.services.ec2.model.VolumeState;
import software.amazon.awssdk.services.ec2.model.VolumeType;

/**
 * Migrates AWS EBS volumes from gp2 to gp3.
 * <ol>
 *   <li>{@link #isAffected} — AWS stack, entitlement on, DB still lists migratable gp2 volumes.</li>
 *   <li>{@link #doApply} — Starts AWS modification and/or polls EC2 until each batch completes.</li>
 * </ol>
 * <p/>
 * The volume conversion process requires multiple passes since volume migration can take up to 24 hours and there can be large numbers of volumes. The job
 * of the first pass is to get a list of all the volumes that need conversion and use the AWS API to initiate the conversion. Any subsequent
 * passes will then monitor the volumes to see if they have either completed successfully or failed. Additionally, each pass of the stack patcher will
 * only process a limited number of volumes. This is for the following reasons:
 * <ol>
 *   <li>Filters provided to the AWS API have a maximum number of values that can be provided</li>
 *   <li>Limits the number of AWS API calls that are needed so the API doesn't get throttled</li>
 *   <li>Eliminates an unbounded state management string we store in the DB</li>
 * </ol>
 * <p/>
 * Runs over multiple scheduler passes: modification can take a long time and each pass caps how many volumes are
 * described or converted (see batch limit constant) so Describe/Modify APIs stay within filter sizes and throttle limits.
 * <p/>
 * State is tracked with {@link DetailedStackStatus} (migration started → in progress → complete/failed) and with
 * {@link CloudVolumeModificationState} on volume entries inside AWS_VOLUMESET / AWS_ROOT_DISK resource attributes.
 * <p/>
 * Rules:
 * <ol>
 *   <li>If {@link #doApply} returns {@code false}, the job runs again (still optimizing, more batches, or partial batch).</li>
 *   <li>If every volume in an initiation attempt fails (no successes, at least one error), patch fails via
 *       {@link ExistingStackPatchApplyException}.</li>
 *   <li>When nothing remains to migrate and monitoring is finished, {@link #doApply} returns {@code true}.</li>
 *   <li>Completed volumes update DB metadata to gp3 and emit usage.</li>
 * </ol>
 */
@Service
public class AwsGp2ToGp3PatchService extends ExistingStackPatchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsGp2ToGp3PatchService.class);

    private static final int VOLUME_BATCH_LIMIT = 50;

    @Inject
    private AwsCommonDiskUpdateService awsCommonDiskUpdateService;

    @Inject
    private CloudConnectorHelper cloudConnectorHelper;

    @Inject
    private ResourceService resourceService;

    @Inject
    private StackStatusService stackStatusService;

    @Inject
    private ResourceAttributeUtil resourceAttributeUtil;

    @Inject
    private StackService stackService;

    @Inject
    private AwsVolumeIopsCalculator volumeIopsCalculator;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private StackPatchUsageReporterService stackPatchUsageReporterService;

    /**
     * Returns the type of stack patch this service handles.
     * 
     * @return AWS_GP2_TO_GP3_MIGRATION patch type
     */
    @Override
    public StackPatchType getStackPatchType() {
        return AWS_GP2_TO_GP3_MIGRATION;
    }

    /**
     * Determines if the stack is affected by this patch (has GP2 volumes that need migration).
     * Only AWS stacks with GP2 volumes in the database are considered affected.
     * 
     * @param stack the stack to check
     * @return true if the stack has GP2 volumes that need migration, false otherwise
     */
    @Override
    public boolean isAffected(Stack stack) {
        if (!CloudPlatform.AWS.name().equalsIgnoreCase(stack.getCloudPlatform())) {
            return false;
        }

        LOGGER.debug("Calling isAffected() for stack {}", stack.getId());
        Collection<Resource> resources = resourceService.findAllByStackIdAndResourceTypeIn(stack.getId(),
                List.of(ResourceType.AWS_ROOT_DISK, ResourceType.AWS_VOLUMESET));
        List<VolumeSetAttributes.Volume> gp2Volumes = getGp2VolumesFromResources(resources);
        return !gp2Volumes.isEmpty();
    }

    /**
     * Applies the GP2 to GP3 migration patch to the stack.
     * Checks if migration has already been started and either continues monitoring existing
     * migrations or starts a new migration process.
     * 
     * @param inputStack the stack to apply the patch to
     * @return true if the patch process is complete, false if another pass is needed
     * @throws ExistingStackPatchApplyException if the patch application fails
     */
    @Override
    protected boolean doApply(Stack inputStack) throws ExistingStackPatchApplyException {
        LOGGER.info("Running GP2 to GP3 volume migration for stack {}", inputStack.getResourceCrn());

        // Make sure the entitlement is enabled for the stack's account
        String accountId = Crn.safeFromString(inputStack.getResourceCrn()).getAccountId();
        if (!entitlementService.isGp2toGp3MigrationEnabled(accountId)) {
            LOGGER.info("Entitlement for GP2 to GP3 volume migration is not enabled for account {}", accountId);
            return false;
        }

        List<StackStatus> statusList = stackStatusService.findAllStackStatusesById(inputStack.getId());
        AmazonEc2Client awsClient = getAwsClient(inputStack);

        boolean hasBeenStarted = isStackPatchProcessStarted(statusList);
        if (hasBeenStarted) {
            LOGGER.info("Reviewing volume status for stack patcher");
            return checkVolumeMigration(inputStack, statusList, awsClient);
        } else {
            LOGGER.info("Starting patcher");
            return startPatchProcess(inputStack, awsClient);
        }
    }

    /**
     * Starts the migration process. This is called the first time the stack patcher is run for a stack,
     * or when starting a new batch of volumes after a previous batch completes.
     *
     * @param inputStack the Stack object that was passed to the doApply method
     * @param awsClient the AWS EC2 client to use for volume operations
     * @return {@code true} if there is nothing left to migrate; {@code false} if another scheduler pass is needed
     * @throws ExistingStackPatchApplyException if initiation fails wholly or something else prevents continuing
     */
    private boolean startPatchProcess(Stack inputStack, AmazonEc2Client awsClient)
            throws ExistingStackPatchApplyException {
        Collection<Resource> resources = resourceService.findAllByStackIdAndResourceTypeIn(inputStack.getId(),
                List.of(ResourceType.AWS_ROOT_DISK, ResourceType.AWS_VOLUMESET));
        List<VolumeSetAttributes.Volume> validGp2Volumes = getValidatedBatchOfGp2VolumeListForMigration(inputStack, awsClient, resources);

        if (validGp2Volumes.isEmpty()) {
            LOGGER.info("No valid GP2 volumes to migrate for stack {} after validation", inputStack.getResourceCrn());
            return true;
        }

        Stack stack = inputStack;
        stack.setStackStatus(new StackStatus(stack, Status.UPDATE_IN_PROGRESS, format("GP2 migration started for %s GP2 volumes", validGp2Volumes.size()),
                DetailedStackStatus.VOLUME_MIGRATION_STARTED));
        stack = stackService.save(stack);
        stackPatchUsageReporterService.reportUsage(stack, AWS_GP2_TO_GP3_MIGRATION,
                UsageProto.CDPStackPatchEventType.Value.UNSET, format("GP2 migration started for %s GP2 volumes", validGp2Volumes.size()));

        boolean inBatch = false;
        if (validGp2Volumes.size() > VOLUME_BATCH_LIMIT) {
            inBatch = true;
            validGp2Volumes = validGp2Volumes.subList(0, VOLUME_BATCH_LIMIT);
            LOGGER.info("Found more than {} GP2 volumes to migrate for stack {}, limiting to {} volumes",
                    VOLUME_BATCH_LIMIT, stack.getResourceCrn(), VOLUME_BATCH_LIMIT);
        }

        ConversionStatus status = initiateAwsVolumeConversionForStack(inputStack, awsClient, validGp2Volumes);
        updateResourceMetadata(inputStack.getId(), new HashSet<>(status.successes()), Set.of(), status.cloudbreakExceptions().keySet(), resources);

        if (inBatch) {
            // We only processed a subgroup of volumes, so we need to run another iteration. Even if they all failed,
            // we need to attempt the next batch.
            return false;
        }

        // If this point is reached, then there is only one batch.
        if (status.successes().isEmpty() && !status.cloudbreakExceptions().isEmpty()) {
            String message = format("All volumes in batch failed immediately id: %s crn: %s", stack.getId(), stack.getResourceCrn());
            LOGGER.error(message);
            stack.setStackStatus(new StackStatus(stack, Status.AVAILABLE, "Failed to apply GP2 to GP3 migration",
                    DetailedStackStatus.VOLUME_MIGRATION_COMPLETE));
            stackService.save(stack);

            // Nothing to recheck to just return true.
            return true;
        }

        // Return false to re-run the stack patch process to check on in-progress volumes.
        return false;
    }

    /**
     * Called when the stack patch process is run the second time and beyond for a given stack.
     * Checks the migration status of volumes that are in progress and updates the stack status accordingly.
     * If all volumes in the current batch are complete, checks if there are more volumes to migrate.
     * 
     * @param stack the Stack object that was passed to the doApply method
     * @param statusList the list of StackStatus objects for the stack
     * @param awsClient the AWS EC2 client to use for querying volume status
     * @return true if stack patch process should finish or false if another pass is needed for any reason, either the current volumes being processed
     *         need to be rechecked or a new batch of volumes needs to be processed.
     * @throws ExistingStackPatchApplyException if something went wrong and the stack patch process should be marked as failed
     */
    private boolean checkVolumeMigration(Stack stack, List<StackStatus> statusList, AmazonEc2Client awsClient) throws ExistingStackPatchApplyException {
        Collection<Resource> resources = resourceService.findAllByStackIdAndResourceTypeIn(stack.getId(),
                List.of(ResourceType.AWS_ROOT_DISK, ResourceType.AWS_VOLUMESET));
        List<VolumeSetAttributes.Volume> cloudbreakGp2Volumes = getGp2VolumesFromResourcesByState(resources,
                CloudVolumeModificationState.GP2_TO_GP3_IN_PROGRESS);

        boolean anotherPassNeeded = updateVolumeStates(cloudbreakGp2Volumes, awsClient, stack, resources);
        if (anotherPassNeeded) {
            return false;
        } else {
            List<VolumeSetAttributes.Volume> validGp2Volumes = getValidatedBatchOfGp2VolumeListForMigration(stack, awsClient, resources);

            if (validGp2Volumes.isEmpty()) {
                Pair<Long, Long> finalResults = countGp2ToGp3TerminalVolumesFromResources(resources);

                // No more volumes to process, just mark the stack patcher as done.
                stack.setStackStatus(new StackStatus(stack, Status.AVAILABLE,
                        format("GP2 migration finished. Failed: %s Succeeded: %s", finalResults.getLeft(), finalResults.getRight()),
                        DetailedStackStatus.VOLUME_MIGRATION_COMPLETE));
                stack = stackService.save(stack);
                stackPatchUsageReporterService.reportUsage(stack, AWS_GP2_TO_GP3_MIGRATION,
                        UsageProto.CDPStackPatchEventType.Value.UNSET,
                        format("GP2 migration finished. Failed: %s Succeeded: %s", finalResults.getLeft(), finalResults.getRight()));
                return true;
            }

            if (validGp2Volumes.size() > VOLUME_BATCH_LIMIT) {
                validGp2Volumes = validGp2Volumes.subList(0, VOLUME_BATCH_LIMIT);
                LOGGER.info("Found more than {} GP2 volumes to migrate for stack {}, limiting to {} volumes",
                        VOLUME_BATCH_LIMIT, stack.getResourceCrn(), VOLUME_BATCH_LIMIT);
            }

            ConversionStatus status = initiateAwsVolumeConversionForStack(stack, awsClient, validGp2Volumes);
            // Pass a null to so that we get a refreshed list of Resources to update.
            updateResourceMetadata(stack.getId(), new HashSet<>(status.successes()), Set.of(), status.cloudbreakExceptions().keySet(), null);
            // If this point is reached, there was a previous batch so we don't want to mark the stack patch
            // as failed. Just return false, the next pass will summary everything.
            return false;
        }
    }

    /**
     * Updates the migration state of volumes by checking their current status in AWS.
     * Iterates through all volumes that started migration and categorizes them as completed,
     * failed, or still in-progress. Updates the database metadata for any newly completed volumes.
     *
     * @param inProgressVolumes volumes marked in-progress for gp2→gp3 in resource metadata
     * @param awsClient the AWS EC2 client to query volume status
     * @param stack the stack being patched
     * @return true if another pass is needed (volumes still in progress), false if all volumes are complete or failed
     *         or the input List is empty
     * @throws ExistingStackPatchApplyException if unable to retrieve volume information from AWS
     */
    private boolean updateVolumeStates(List<VolumeSetAttributes.Volume> inProgressVolumes,
            AmazonEc2Client awsClient, Stack stack, Collection<Resource> resources) throws ExistingStackPatchApplyException {
        if (inProgressVolumes.isEmpty()) {
            return false;
        }

        Set<String> newlyCompletedList = new HashSet<>();
        Set<String> newlyFailedList = new HashSet<>();
        Map<String, Volume> awsVolumes = getAwsVolumeInfo(inProgressVolumes, awsClient);
        Map<String, VolumeModification> awsVolumeModifications = getLatestVolumeModification(
                inProgressVolumes.stream().map(VolumeSetAttributes.Volume::getId).toList(), awsClient);
        boolean anotherPassNeeded = false;
        for (VolumeSetAttributes.Volume volume : inProgressVolumes) {
            // Iterate through all the started volume IDs and check their status.
            Volume awsVolume = awsVolumes.get(volume.getId());
            if (awsVolume == null) {
                // this shouldn't happen, but if it does, we need further investigation.
                throw new ExistingStackPatchApplyException(format("Unable to get volume info for volume from AWS: %s", volume.getId()));
            }
            MigrationState state = isVolumeMigrationComplete(awsVolume, awsVolumeModifications.get(volume.getId()));
            switch (state) {
                case COMPLETE:
                    newlyCompletedList.add(volume.getId());
                    stackPatchUsageReporterService.reportUsage(stack, AWS_GP2_TO_GP3_MIGRATION, UsageProto.CDPStackPatchEventType.Value.UNSET,
                            "Conversion successful for " + volume.getId());
                    break;
                case FAILED:
                    newlyFailedList.add(volume.getId());
                    stackPatchUsageReporterService.reportUsage(stack, AWS_GP2_TO_GP3_MIGRATION, UsageProto.CDPStackPatchEventType.Value.UNSET,
                            "Conversion failed for " + volume.getId());
                    break;
                case IN_PROGRESS:
                    anotherPassNeeded = true;
                    break;
                default:
                    // The MigrationState enum only has 3 values.
                    throw new IllegalStateException("Unexpected state: " + state);
            }
        }

        updateResourceMetadata(stack.getId(), Set.of(), newlyCompletedList, newlyFailedList, resources);

        return anotherPassNeeded;
    }

    /**
     * Checks to see if the migration is complete for a given volume based on its modification state.
     * 
     * @param awsVolume the AWS Volume object
     * @param modState the latest VolumeModificationState for the volume
     * @return MigrationState indicating COMPLETE, IN_PROGRESS, or FAILED
     */
    private MigrationState isVolumeMigrationComplete(Volume awsVolume, VolumeModification modState) {
        if (modState == null) {
            // This is an edge case where we did not get any modification state from AWS.
            LOGGER.warn("Migration state was empty for volume: {}", awsVolume.volumeId());
            return MigrationState.FAILED;
        }

        VolumeModificationState state = modState.modificationState();
        if (state.equals(VolumeModificationState.MODIFYING) || state.equals(VolumeModificationState.OPTIMIZING)) {
            // Migration is still in progress so we need another pass of this stack patcher.
            return MigrationState.IN_PROGRESS;
        } else if (state.equals(VolumeModificationState.FAILED)) {
            // The migration failed during the process. Add to failed list so we skip on subsequent runs.
            LOGGER.warn("Migration state was failed: {}", modState.statusMessage());
            return MigrationState.FAILED;
        } else if (state.equals(VolumeModificationState.UNKNOWN_TO_SDK_VERSION)) {
            if (awsVolume.state().equals(VolumeState.IN_USE) && awsVolume.volumeType().equals(VolumeType.GP3)) {
                return MigrationState.COMPLETE;
            } else {
                // Since the state is definitely not FAILED and the desired state is not yet met, return IN_PROGRESS
                return MigrationState.IN_PROGRESS;
            }
        } else {
            return MigrationState.COMPLETE;
        }
    }

    /**
     * Represents the current state of a volume migration.
     */
    private enum MigrationState {
        /** Volume migration has completed successfully */
        COMPLETE,
        /** Volume migration is still in progress */
        IN_PROGRESS,
        /** Volume migration has failed */
        FAILED
    }

    /**
     * Determines if a stack patch migration process has been started but not completed.
     * Searches through status history from newest to oldest to find if there's an in-progress
     * migration that hasn't been finalized.
     * <p/>
     * Returns true if VOLUME_MIGRATION_IN_PROGRESS is found before any terminal state
     * (VOLUME_MIGRATION_COMPLETE or VOLUME_MIGRATION_FAILED). Returns false if a terminal
     * state is encountered first, or if no migration statuses exist.
     * 
     * @param statusList the list of stack statuses to check
     * @return true if migration is in progress, false otherwise
     */
    private boolean isStackPatchProcessStarted(List<StackStatus> statusList) {
        List<StackStatus> sorted = statusList.stream()
                .sorted(Comparator.comparing(StackStatus::getCreated))
                .toList();

        for (int i = sorted.size() - 1; i >= 0; i--) {
            switch (sorted.get(i).getDetailedStackStatus()) {
                case VOLUME_MIGRATION_COMPLETE -> {
                    // If we encounter an ending state first, then we know the previous process (if any) has finished
                    // and a new process should start.
                    return false;
                }
                case VOLUME_MIGRATION_STARTED -> {
                    // If we encounter a starting state first, then we know the stack patch process has started.
                    return true;
                }
                default -> {
                    // do nothing. move to the next status.
                }
            }
        }

        // We didn't encounter any relevant states so return false.
        return false;
    }

    /**
     * Returns the latest volume modification state for a given volume ID by querying AWS.
     * 
     * @param volumeIds a List of volume IDs to check
     * @param awsClient the AWS EC2 client to use
     * @return the latest volume modification state, or empty if no modification state is found
     */
    private Map<String, VolumeModification> getLatestVolumeModification(List<String> volumeIds, AmazonEc2Client awsClient) {
        DescribeVolumesModificationsRequest request = DescribeVolumesModificationsRequest.builder()
                .volumeIds(volumeIds)
                .build();

        DescribeVolumesModificationsResponse response = awsClient.describeVolumeModifications(request);
        if (response.volumesModifications().isEmpty()) {
            return new HashMap<>();
        } else {
            return response.volumesModifications().stream().collect(Collectors.toMap(VolumeModification::volumeId, Function.identity()));
        }

    }

    /**
     * Returns a list of GP2 volumes that are valid and exist in AWS. The volumes are filtered to only include volumes that:
     * - Have a valid ID
     * - Have a valid size
     * - Exist in AWS
     * - Are GP2 in AWS
     * - Are attached to a VM (IN_USE state)
     * <p/>
     * Returns every migratable gp2 volume that passes validation (no batch cap here; callers limit to
     * {@link #VOLUME_BATCH_LIMIT} when calling EC2 modify).
     * 
     * @param stack the stack to get the GP2 volumes from
     * @param awsClient the AWS EC2 client to use for querying volume state
     * @return a list of GP2 volumes that are valid and exist in AWS, or an empty list if no valid volumes are found
     */
    private List<VolumeSetAttributes.Volume> getValidatedBatchOfGp2VolumeListForMigration(Stack stack, AmazonEc2Client awsClient,
            Collection<Resource> resources) {
        List<VolumeSetAttributes.Volume> cloudbreakGp2Volumes = getGp2VolumesFromResources(resources);

        Map<String, Volume> awsVolumeMap = getAwsVolumeInfo(cloudbreakGp2Volumes, awsClient);

        // Filter only volumes that actually exist in AWS and are GP2
        List<VolumeSetAttributes.Volume> validGp2Volumes = cloudbreakGp2Volumes.stream()
                .filter(volume -> {
                    if (volume.getId() == null || volume.getId().isEmpty()) {
                        // Volume ID is empty, don't include since we need the ID later. Should never happen, but
                        // validating input to make sure.
                        LOGGER.warn("Volume ID was empty in cloudbreak DB.");
                        return false;
                    }
                    if (volume.getSize() == null || volume.getSize() <= 0) {
                        // Volume size is empty, don't include since we need the size later. Should never happen, but
                        // validating input to make sure.
                        LOGGER.warn("Volume size was empty in cloudbreak DB.");
                        return false;
                    }
                    Volume awsVolume = awsVolumeMap.get(volume.getId());
                    if (awsVolume == null) {
                        // The volume is unknown to AWS, don't include. Should never happen, but
                        // validating input to make sure.
                        LOGGER.warn("Volume in cloudbreak DB is not known by AWS. volumeID: {}", volume.getId());
                        return false;
                    }
                    if (VolumeType.GP2 != awsVolume.volumeType()) {
                        // The volume in AWS not GP2, don't include. Should never happen, but
                        // validating input to make sure.
                        LOGGER.warn("GP2 volume in cloudbreak DB is not GP2 in AWS. volumeID: {} awsType: {}", volume.getId(), awsVolume.volumeType());
                        return false;
                    }
                    if (VolumeState.IN_USE != awsVolume.state()) {
                        // The volume state is not attached to a VM so we'll skip it.
                        // See https://docs.aws.amazon.com/ebs/latest/userguide/ebs-describing-volumes.html#volume-state for more information.
                        // We don't want to migrate volumes that are not attached to a VM (e.g. AVAILABLE) or volumes that have
                        // a state of ERROR, CREATING, etc.
                        LOGGER.warn("AWS volume state is not IN_USE. volumeID: {} awsState: {}", volume.getId(), awsVolume.state());
                        return false;
                    }
                    return true;
                })
                .toList();

        return validGp2Volumes;
    }

    /**
     * Initiates the AWS volume conversion from GP2 to GP3 for a stack.
     * Calculates GP3 IOPS per volume and calls EC2 modify for each id. Volumes are processed
     * one at a time to reduce throttling.
     *
     * @param stack the stack (for logging and usage on errors)
     * @param awsClient the AWS EC2 client used to perform volume modifications
     * @param volumesToMigrate gp2 volumes to convert (callers supply a batch with valid ids and sizes)
     * @return successes and per-volume {@link CloudbreakException}s for failed initiations
     */
    private ConversionStatus initiateAwsVolumeConversionForStack(Stack stack, AmazonEc2Client awsClient, List<VolumeSetAttributes.Volume> volumesToMigrate) {
        Map<String, Integer> volumeIopsMap = volumesToMigrate.stream()
                .collect(Collectors.toMap(
                        VolumeSetAttributes.Volume::getId,
                        volume -> {
                            int targetIops = volumeIopsCalculator.getEquivalentGp3IopsForGp2Volume(volume.getSize());
                            LOGGER.debug("Volume {} ({} GB) will be migrated with {} IOPS", volume.getId(), volume.getSize(), targetIops);
                            return targetIops;
                        }
                ));

        // Perform the migration one volume at a time
        ConversionStatus conversionStatus = new ConversionStatus(new HashMap<>(), new ArrayList<>());
        for (Map.Entry<String, Integer> entry : volumeIopsMap.entrySet()) {
            String volumeId = entry.getKey();
            Integer iops = entry.getValue();
            LOGGER.info("Converting volume {} to GP3 with {} IOPS", volumeId, iops);

            // Modify single volume
            try {
                awsCommonDiskUpdateService.modifyVolumesWithIops(
                        awsClient,
                        volumeId,
                        VolumeType.GP3.toString(),
                        iops);
                conversionStatus.successes().add(volumeId);
            } catch (CloudbreakException e) {
                String message = format("Migration AWS error for volume. volume id: %s resource crn: %s", volumeId, stack.getResourceCrn());
                LOGGER.error(message, e);
                conversionStatus.cloudbreakExceptions().put(volumeId, e);
                stackPatchUsageReporterService.reportUsage(stack, AWS_GP2_TO_GP3_MIGRATION, UsageProto.CDPStackPatchEventType.Value.UNSET,
                        "Error starting conversion for " + volumeId + ": " + e.getMessage());
            }
        }
        return conversionStatus;
    }

    /**
     * Determines if the patch should check for failed retryable flows before applying.
     * Volume modifications don't start flows, so we can proceed even if there are failed flows.
     * 
     * @return false, as volume modifications don't require flow checks
     */
    @Override
    protected boolean shouldCheckForFailedRetryableFlow() {
        return false;
    }

    /**
     * Updates persisted {@link VolumeSetAttributes} on stack resources after initiation or polling.
     * Marks volumes in-progress after successful modify initiation, finished (and gp3 type) when done,
     * or failed when initiation or migration fails.
     *
     * @param stackId the stack owning the resources
     * @param inProgressList volume ids that just started modifying
     * @param finishedList volume ids confirmed migrated to gp3
     * @param failedList volume ids that failed initiation or aws migration
     */
    private void updateResourceMetadata(Long stackId, Set<String> inProgressList, Set<String> finishedList,
            Set<String> failedList, Collection<Resource> resources) {
        if (resources == null || resources.isEmpty()) {
            resources = resourceService.findAllByStackIdAndResourceTypeIn(stackId,
                    List.of(ResourceType.AWS_ROOT_DISK, ResourceType.AWS_VOLUMESET));
        }

        List<Resource> resourcesToSave = new ArrayList<>();
        for (Resource res : resources) {
            boolean saveNeeded = false;
            Optional<VolumeSetAttributes> volumeSetOptional = resourceAttributeUtil.getTypedAttributes(res, VolumeSetAttributes.class);
            if (volumeSetOptional.isPresent()) {
                VolumeSetAttributes volumeSet = volumeSetOptional.get();
                List<VolumeSetAttributes.Volume> existingVolumes = volumeSet.getVolumes();
                if (existingVolumes != null) {
                    saveNeeded = updateVolumeInformation(inProgressList, finishedList, failedList, existingVolumes, volumeSet, res);
                }
            }
            if (saveNeeded) {
                resourcesToSave.add(res);
            }
        }

        if (!resourcesToSave.isEmpty()) {
            List<String> ids = resourcesToSave.stream().map(Resource::getResourceName).toList();
            resourceService.saveAll(resourcesToSave);
            LOGGER.info("Successfully saved resources {}", ids);
        }
    }

    /**
     * Used to store the state of the AWS gp2→gp3 modification outcome in the appropriate {@link VolumeSetAttributes.Volume}
     * by setting the modification state on the volume. The following logic is implemented:
     * <p/>
     * If volume in  {@code inProgressList} -> set modification state to GP2_TO_GP3_IN_PROGRESS<br/>
     * If volume in  {@code failedList} -> set modification state to GP2_TO_GP3_FAILED<br/>
     * If volume in  {@code finishedList} -> set modification state to GP2_TO_GP3_FINISHED and volume type to GP3<br/>
     *
     * @return true if any volume was updated (caller should persist {@code res})
     */
    private boolean updateVolumeInformation(Set<String> inProgressList, Set<String> finishedList,
            Set<String> failedList, List<VolumeSetAttributes.Volume> existingVolumes, VolumeSetAttributes volumeSet,
            Resource res) {
        boolean changesMade = false;
        for (VolumeSetAttributes.Volume volume : existingVolumes) {
            if (inProgressList.contains(volume.getId())) {
                volume.setModificationState(CloudVolumeModificationState.GP2_TO_GP3_IN_PROGRESS);
                res.setAttributes(new Json(volumeSet));
                LOGGER.info("Updating volume to in-progress - {}", volume.getId());
                changesMade = true;
            } else if (failedList.contains(volume.getId())) {
                volume.setModificationState(CloudVolumeModificationState.GP2_TO_GP3_FAILED);
                res.setAttributes(new Json(volumeSet));
                LOGGER.info("Updating volume to failed - {}", volume.getId());
                changesMade = true;
            } else if (finishedList.contains(volume.getId())) {
                volume.setModificationState(CloudVolumeModificationState.GP2_TO_GP3_FINISHED);
                volume.setType(VolumeType.GP3.toString());
                res.setAttributes(new Json(volumeSet));
                LOGGER.info("Updating volume to finished - {}", volume.getId());
                changesMade = true;
            }
        }
        return changesMade;
    }

    /**
     * Describes the given volumes in EC2 and returns a map keyed by aws volume id.
     *
     * @param volumes cloudbreak-side volume rows (null or empty ids are skipped)
     * @param awsClient EC2 client
     * @return volume id → EC2 Volume; missing ids omitted (empty response yields empty map)
     */
    private Map<String, Volume> getAwsVolumeInfo(List<VolumeSetAttributes.Volume> volumes, AmazonEc2Client awsClient) {
        if (volumes.isEmpty()) {
            return new HashMap<>();
        }
        LOGGER.debug("Querying {} volumes in EC2", volumes.size());

        List<String> volumeIdStrings = volumes.stream()
                .map(VolumeSetAttributes.Volume::getId)
                .filter(id -> id != null && !id.isEmpty())
                .toList();
        if (volumeIdStrings.isEmpty()) {
            return new HashMap<>();
        }

        DescribeVolumesRequest describeVolumeRequest = DescribeVolumesRequest.builder()
                .volumeIds(volumeIdStrings)
                .build();
        DescribeVolumesResponse response = awsClient.describeVolumes(describeVolumeRequest);
        if (response.volumes().isEmpty()) {
            return new HashMap<>();
        } else {
            return response.volumes().stream().collect(Collectors.toMap(Volume::volumeId, Function.identity()));
        }
    }

    /**
     * Creates and returns an AWS EC2 client for the given stack.
     * 
     * @param stack the stack to create the client for
     * @return the authenticated AWS EC2 client
     */
    @VisibleForTesting
    public AmazonEc2Client getAwsClient(Stack stack) {
        CloudConnectResources ccr = cloudConnectorHelper.getCloudConnectorResources(stack);
        AuthenticatedContext authContext = ccr.getAuthenticatedContext();
        return new AuthenticatedContextView(authContext).getAmazonEC2Client();
    }

    /**
     * Retrieves GP2 volumes from the stack's resource metadata stored in the database.
     * This method examines AWS_VOLUMESET and AWS_ROOT_DISK resources to identify volumes
     * with GP2 type.
     *
     * NOTE: This returns database metadata which may be stale if volumes were modified
     * outside of Cloudbreak. Always validate against actual AWS state before making changes.
     *
     * @return a list of GP2 volumes found in the stack's resource metadata
     */
    @VisibleForTesting
    List<VolumeSetAttributes.Volume> getGp2VolumesFromResources(Collection<Resource> resources) {
        LOGGER.debug("Checking {} resources for unprocessed GP2 volumes", resources.size());

        List<VolumeSetAttributes.Volume> gp2Volumes = resources.stream()
                .map(resourceAttributeUtil::<VolumeSetAttributes>getTypedAttributes)
                .flatMap(Optional::stream)
                .map(VolumeSetAttributes::getVolumes)
                .flatMap(List::stream)
                .filter(volume -> AwsDiskType.Gp2.toString().equalsIgnoreCase(volume.getType()))
                .filter(volume -> volume.getModificationState() == null)
                .collect(Collectors.toList());

        LOGGER.debug("Found {} GP2 volumes from stack resources", gp2Volumes.size());
        return gp2Volumes;
    }

    /**
     * Retrieves GP2 volumes from the stack's resource metadata stored in the database.
     * This method examines AWS_VOLUMESET and AWS_ROOT_DISK resources to identify volumes
     * with GP2 type.
     *
     * NOTE: This returns database metadata which may be stale if volumes were modified
     * outside of Cloudbreak. Always validate against actual AWS state before making changes.
     *
     * @param resources stack resources to scan
     * @param modificationState modification state to match (e.g. in-progress)
     * @return matching gp2 volumes from resource metadata
     */
    @VisibleForTesting
    List<VolumeSetAttributes.Volume> getGp2VolumesFromResourcesByState(Collection<Resource> resources, CloudVolumeModificationState modificationState) {
        LOGGER.debug("Checking {} resources for GP2 volumes by state {}", resources.size(), modificationState.name());

        List<VolumeSetAttributes.Volume> gp2Volumes = resources.stream()
                .map(resourceAttributeUtil::<VolumeSetAttributes>getTypedAttributes)
                .flatMap(Optional::stream)
                .map(VolumeSetAttributes::getVolumes)
                .flatMap(List::stream)
                .filter(volume -> AwsDiskType.Gp2.toString().equalsIgnoreCase(volume.getType()))
                .filter(volume -> (volume.getModificationState() != null) && (volume.getModificationState().equals(modificationState)))
                .collect(Collectors.toList());

        LOGGER.debug("Found {} GP2 volumes from stack resources", gp2Volumes.size());
        return gp2Volumes;
    }

    /**
     * Counts volumes in terminal GP2→GP3 migration states from stack resource metadata (same resource
     * traversal as {@link #getGp2VolumesFromResourcesByState}).
     *
     * @param resources stack resources to scan
     * @return left: volumes with {@link CloudVolumeModificationState#GP2_TO_GP3_FAILED} and type Gp2;
     *         right: volumes with {@link CloudVolumeModificationState#GP2_TO_GP3_FINISHED} and type Gp3
     */
    private Pair<Long, Long> countGp2ToGp3TerminalVolumesFromResources(Collection<Resource> resources) {
        LOGGER.debug("Counting GP2 to GP3 terminal-state volumes across {} resources", resources.size());

        List<VolumeSetAttributes.Volume> volumes = resources.stream()
                .map(resourceAttributeUtil::<VolumeSetAttributes>getTypedAttributes)
                .flatMap(Optional::stream)
                .map(VolumeSetAttributes::getVolumes)
                .flatMap(List::stream)
                .toList();

        long failedGp2Count = volumes.stream()
                .filter(volume -> CloudVolumeModificationState.GP2_TO_GP3_FAILED.equals(volume.getModificationState()))
                .filter(volume -> AwsDiskType.Gp2.toString().equalsIgnoreCase(volume.getType()))
                .count();

        long finishedGp3Count = volumes.stream()
                .filter(volume -> CloudVolumeModificationState.GP2_TO_GP3_FINISHED.equals(volume.getModificationState()))
                .filter(volume -> AwsDiskType.Gp3.toString().equalsIgnoreCase(volume.getType()))
                .count();

        LOGGER.debug("Found {} GP2_TO_GP3_FAILED Gp2 volumes and {} GP2_TO_GP3_FINISHED Gp3 volumes",
                failedGp2Count, finishedGp3Count);

        return Pair.of(failedGp2Count, finishedGp3Count);
    }

}
