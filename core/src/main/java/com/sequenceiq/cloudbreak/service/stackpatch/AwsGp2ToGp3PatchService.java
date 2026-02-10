package com.sequenceiq.cloudbreak.service.stackpatch;

import static com.sequenceiq.cloudbreak.domain.stack.StackPatchType.AWS_GP2_TO_GP3_MIGRATION;
import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Arrays;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
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
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.common.api.type.ResourceType;

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
 * This service is responsible for migrating AWS EBS volumes from gp2 to gp3 volume types. The general flow of the service is:
 * 1. Check if the stack is affected by the migration. A stack is affected if the local database has a record of any gp2 volumes.
 * 2. If the stack is affected, initiate the volume conversion.
 * <br/>
 * The volume conversion process requires multiple passes since volume migration can take up to 24 hours and there can be large numbers of volumes. The job
 * of the first pass is to get a list of all the volumes that need conversion and use the AWS API to initiate the conversion. Any subsequent
 * passes will then monitor the volumes to see if they have either completed successfully or failed. Additionally, each pass of the stack patcher will
 * only process a limited number of volumes. This is for the following reasons:
 * 1. Filters provided to the AWS API have a maximum number of values that can be provided
 * 2. Limits the number of AWS API calls that are needed so the API doesn't get throttled
 * 3. Eliminiates an unbounded state management string we store in the DB
 * <br/>
 * To keep track of which volumes need monitoring and which volumes failed or succeeded conversion this patch process uses the statusReason property of the
 * StackStatus object. The format is: started:vol1,vol2,vol3|failed:vol1,vol2|completed:vol1,vol2 - storing the complete list of volumes that started migration
 * and the list of volumes that failed (either to initiate or during migration). Every run of this process when hasBeenStarted is true must first get
 * the last UPDATE_IN_PROGRESS status and parse out which volumes to check (those in started that are not in failed) and which to
 * skip (those in a failed state).
 * <br/>
 * Some important rules:
 * 1. if doApply returns false, the process will execute again in the future. This should be used if volume migration is still in progress, e.g. if any
 *    of the volumes are still listed as "modifying" or "optimizing".
 * 2. If all the volumes failed to initiate the migration, the doApply should throw a new CloudbreakRuntimeException exception to make the stack process
 *    as failed.
 * 3. If all the volumes were migrated successfully, then the doApply should return true.
 * 4. Once a volume is successfully migrated, the volume information should be updated in the database and a usage event should be sent to
 *    record the success.
 * <br/>
 * Criteria for successful migration:
 * 1. The volume state is set to "completed" and volume type is listed as "gp3"
 */
@Service
public class AwsGp2ToGp3PatchService extends ExistingStackPatchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsGp2ToGp3PatchService.class);

    private static final String STARTED_PREFIX = "started:";

    private static final String FAILED_PREFIX = "failed:";

    private static final String COMPLETED_PREFIX = "completed:";

    private static final String PART_SEPARATOR = "|";

    private static final int NUM_STATE_PARTS = 3;

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
    private StackUtil stackUtil;

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

        // Make sure the entitlement is enabled for the stack's account
        String accountId = Crn.safeFromString(stack.getResourceCrn()).getAccountId();
        if (!entitlementService.isGp2toGp3MigrationEnabled(stack.getResourceCrn())) {
            return false;
        }
        LOGGER.debug("Calling isAffected() for stack {}", stack.getId());
        Collection<Resource> resources = resourceService.findAllByStackIdAndResourceTypeIn(stack.getId(),
                List.of(ResourceType.AWS_ROOT_DISK, ResourceType.AWS_VOLUMESET));
        List<VolumeSetAttributes.Volume> gp2Volumes = stackUtil.getGp2VolumesFromResources(resources);
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
        LOGGER.info("Starting GP2 to GP3 volume migration for stack {}", inputStack.getResourceCrn());

        List<StackStatus> statusList = stackStatusService.findAllStackStatusesById(inputStack.getId());
        AmazonEc2Client awsClient = getAwsClient(inputStack);

        boolean hasBeenStarted = isStackPatchProcessStarted(statusList);
        if (hasBeenStarted) {
            LOGGER.debug("Reviewing volume status for stack patcher");
            return checkVolumeMigration(inputStack, statusList, awsClient);
        } else {
            LOGGER.debug("Starting patcher");
            return startPatchProcess(inputStack, awsClient, null);
        }
    }

    /**
     * Starts the migration process. This is called the first time the stack patcher is run for a stack,
     * or when starting a new batch of volumes after a previous batch completes.
     * 
     * @param inputStack the Stack object that was passed to the doApply method
     * @param awsClient the AWS EC2 client to use for volume operations
     * @param volumeIdsToProcess optional list of specific volumes to process; if null, all eligible volumes are processed
     * @return true if stack patch process should finish or false if another pass is needed
     * @throws ExistingStackPatchApplyException if something went wrong and the stack patch process should be marked as failed
     */
    private boolean startPatchProcess(Stack inputStack, AmazonEc2Client awsClient, List<VolumeSetAttributes.Volume> volumeIdsToProcess)
            throws ExistingStackPatchApplyException {
        Stack stack = inputStack;
        stack.setStackStatus(new StackStatus(stack, Status.UPDATE_REQUESTED, "GP2 migration started",
                DetailedStackStatus.VOLUME_MIGRATION_STARTED));
        // We are going to update the stack status again so we need to update the stack object with the new status.
        stack = stackService.save(stack);
        stackPatchUsageReporterService.reportUsage(stack, AWS_GP2_TO_GP3_MIGRATION,
                UsageProto.CDPStackPatchEventType.Value.UNSET, "GP2 migration started");

        // This is a new stack patch job for the stack so we'll start from the beginning.
        // The initiateVolumeConversionForStack() method will return a ConversionStatus object that contains the status of the volume conversion.
        ConversionStatus status = initiateAwsVolumeConversionForStack(inputStack, awsClient, volumeIdsToProcess);

        if (!status.cloudbreakExceptions().isEmpty() && status.successes().isEmpty()) {
            // If all the volumes threw an error then just make the patcher as Failed. We'll log a message,
            // update the status, and then throw an error.
            String message = format("Failed to apply GP2 to GP3 migration for stack %s: %s",
                    stack.getResourceCrn(), stack.getId());
            LOGGER.error(message);
            String stateMessage = buildMigrationStateMessage(List.of(), new ArrayList<>(status.cloudbreakExceptions().keySet()), List.of());
            stack.setStackStatus(new StackStatus(stack, Status.UPDATE_FAILED, stateMessage,
                    DetailedStackStatus.VOLUME_MIGRATION_FAILED));
            stackService.save(stack);
            throw new ExistingStackPatchApplyException(message);
        } else if (status.cloudbreakExceptions().isEmpty() && !status.successes().isEmpty()) {
            // No exceptions were thrown. Return false so the stackpatcher gets marked as SKIPPED and reviewed again. Store the list
            // of volumes that got started in the status message.
            LOGGER.debug("All the migrations were started.");
            String stateMessage = buildMigrationStateMessage(status.successes(), List.of(), List.of());
            stack.setStackStatus(new StackStatus(stack, Status.UPDATE_IN_PROGRESS, stateMessage,
                    DetailedStackStatus.VOLUME_MIGRATION_IN_PROGRESS));
            stackService.save(stack);
            return false;
        } else if (status.cloudbreakExceptions().isEmpty() && status.successes().isEmpty()) {
            // both are empty, so there were no gp2 volumes to convert. Just return true.
            LOGGER.info(format("No GP2 volumes were found to migrate for stack %s.", stack.getId()));
            stack.setStackStatus(new StackStatus(stack, Status.UPDATE_FAILED, "No valid GP2 volumes found",
                    DetailedStackStatus.VOLUME_MIGRATION_FAILED));
            stackService.save(stack);
            return true;
        } else {
            // Return false so the stackpatcher gets marked as SKIPPED and reviewed again. Even though some failed we need to check the rest.
            // Store both started and failed volume lists in the status reason for subsequent runs to parse.
            String message = format("Failed to apply some of GP2 to GP3 migrations for stack %s: %s",
                    stack.getResourceCrn(), stack.getId());
            LOGGER.error(message);
            String stateMessage = buildMigrationStateMessage(status.successes(), new ArrayList<>(status.cloudbreakExceptions().keySet()), List.of());
            stack.setStackStatus(new StackStatus(stack, Status.UPDATE_IN_PROGRESS, stateMessage,
                    DetailedStackStatus.VOLUME_MIGRATION_IN_PROGRESS));
            stackService.save(stack);
            return false;
        }
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
        // Parse the last UPDATE_IN_PROGRESS status to get volumes that started migration and volumes that failed
        MigrationVolumeState migrationState = parseMigrationStateFromStatus(statusList);
        boolean anotherPassNeeded = updateVolumeStates(migrationState, awsClient, stack);
        String stateMessage = buildMigrationStateMessage(migrationState.startedVolumeIds(),
                migrationState.failedVolumeIds(), migrationState.completedVolumeIds());
        if (anotherPassNeeded) {
            stack.setStackStatus(new StackStatus(stack, Status.UPDATE_IN_PROGRESS, stateMessage,
                    DetailedStackStatus.VOLUME_MIGRATION_IN_PROGRESS));
            stackService.save(stack);
            return false;
        } else {
            // All the conversions finished.
            stack.setStackStatus(new StackStatus(stack, Status.AVAILABLE, stateMessage,
                    DetailedStackStatus.VOLUME_MIGRATION_COMPLETE));
            stackService.save(stack);

            // The current batch was finished, check to see if another batch of volumes needs to be processed.
            List<VolumeSetAttributes.Volume> nextBatch = getNextBatch(stack, awsClient);
            if  (nextBatch.isEmpty()) {
                // There are no volumes remaining.
                stackPatchUsageReporterService.reportUsage(stack, AWS_GP2_TO_GP3_MIGRATION,
                        UsageProto.CDPStackPatchEventType.Value.UNSET, "GP2 migration finished");
                return true;
            }

            stackPatchUsageReporterService.reportUsage(stack, AWS_GP2_TO_GP3_MIGRATION,
                    UsageProto.CDPStackPatchEventType.Value.UNSET, "GP2 migration moving to next batch");

            // There is a new batch of volumes that need to be processed.
            return startPatchProcess(stack, awsClient, nextBatch);
        }
    }

    /**
     * Updates the migration state of volumes by checking their current status in AWS.
     * Iterates through all volumes that started migration and categorizes them as completed,
     * failed, or still in-progress. Updates the database metadata for any newly completed volumes.
     * 
     * @param migrationState the current migration state with lists of started, failed, and completed volume IDs
     * @param awsClient the AWS EC2 client to query volume status
     * @param stack the stack being patched
     * @return true if another pass is needed (volumes still in progress), false if all volumes are complete or failed
     * @throws ExistingStackPatchApplyException if unable to retrieve volume information from AWS
     */
    private boolean updateVolumeStates(MigrationVolumeState migrationState, AmazonEc2Client awsClient, Stack stack) throws ExistingStackPatchApplyException {
        List<String> newStartedList = new ArrayList<>();
        List<String> newlyCompletedList = new ArrayList<>();
        boolean anotherPassNeeded = false;
        for (String volumeId : migrationState.startedVolumeIds()) {
            // Iterate through all the started volume IDs and check their status.
            Optional<Volume> awsVolume = getAwsVolumeInfo(volumeId, awsClient);
            if  (awsVolume.isEmpty()) {
                // this shouldn't happen, but if it does, we need further investigation.
                throw new ExistingStackPatchApplyException(format("Unable to get volume info for volume from AWS: %s", volumeId));
            }
            MigrationState state = isVolumeMigrationComplete(awsVolume.get(), getLatestVolumeModification(volumeId, awsClient));
            switch (state) {
                case COMPLETE:
                    // Add to completed list. The completed list can only grow.
                    migrationState.completedVolumeIds().add(volumeId);
                    newlyCompletedList.add(volumeId);
                    break;
                case FAILED:
                    // Add to the failed list. The failed list can only grow.
                    migrationState.failedVolumeIds().add(volumeId);
                    stackPatchUsageReporterService.reportUsage(stack, AWS_GP2_TO_GP3_MIGRATION, UsageProto.CDPStackPatchEventType.Value.UNSET,
                            "Conversion failed for " + volumeId);
                    break;
                case IN_PROGRESS:
                    // Add to the new list for volumes that are still in-progress. This list should either shrink or stay the same.
                    newStartedList.add(volumeId);
                    anotherPassNeeded = true;
                    break;
                default:
                    // The MigrationState enum only has 3 values.
                    throw new IllegalStateException("Unexpected state: " + state);
            }
        }

        // Update the migration state with the new list of in-progress volumes.
        migrationState.startedVolumeIds().clear();
        migrationState.startedVolumeIds().addAll(newStartedList);

        for (String volumeId : newlyCompletedList) {
            stackPatchUsageReporterService.reportUsage(stack, AWS_GP2_TO_GP3_MIGRATION, UsageProto.CDPStackPatchEventType.Value.UNSET,
                    "Conversion successful for " + volumeId);
            updateResourceMetadata(stack.getId(), volumeId);
        }

        return anotherPassNeeded;
    }

    /**
     * Retrieves the next batch of GP2 volumes to migrate for the given stack.
     * This method excludes volumes that have already been processed in previous migration runs
     * and limits the batch size to VOLUME_BATCH_LIMIT to prevent AWS API throttling.
     * 
     * @param stack the stack to get volumes from
     * @param awsClient the AWS EC2 client to validate volumes
     * @return a list of validated GP2 volumes to migrate, up to VOLUME_BATCH_LIMIT in size
     */
    private List<VolumeSetAttributes.Volume> getNextBatch(Stack stack, AmazonEc2Client awsClient) {
        // Step 1: Get a list of GP2 volumes that Cloudbreak has a record of.
        List<VolumeSetAttributes.Volume> validGp2Volumes = getValidatedGp2VolumeList(stack, awsClient);

        // Step 2: Volume IDs already recorded from past completed migration runs.
        Set<String> visitedIds = parseCompletePastMigrationStateFromStatus(stack.getId());

        List<VolumeSetAttributes.Volume> batch = new ArrayList<>();
        for (VolumeSetAttributes.Volume volume : validGp2Volumes) {
            if (batch.size() >= VOLUME_BATCH_LIMIT) {
                break;
            }
            String volumeId = volume.getId();
            if (volumeId != null && !visitedIds.contains(volumeId)) {
                batch.add(volume);
            }
        }
        return batch;
    }

    /**
     * Checks to see if the migration is complete for a given volume based on its modification state.
     * 
     * @param awsVolume the AWS Volume object
     * @param modState the latest VolumeModificationState for the volume
     * @return MigrationState indicating COMPLETE, IN_PROGRESS, or FAILED
     */
    private MigrationState isVolumeMigrationComplete(Volume awsVolume, Optional<VolumeModification> modState) {
        if (modState.isEmpty()) {
            // This is an edge case where we did not get any modification state from AWS.
            LOGGER.warn("Migration state was empty");
            return MigrationState.FAILED;
        }

        VolumeModificationState state = modState.get().modificationState();
        if (state.equals(VolumeModificationState.MODIFYING) || state.equals(VolumeModificationState.OPTIMIZING)) {
            // Migration is still in progress so we need another pass of this stack patcher.
            return MigrationState.IN_PROGRESS;
        } else if (state.equals(VolumeModificationState.FAILED)) {
            // The migration failed during the process. Add to failed list so we skip on subsequent runs.
            LOGGER.warn("Migration state was failed: {}", modState.get().statusMessage());
            return MigrationState.FAILED;
        } else if (state.equals(VolumeModificationState.UNKNOWN_TO_SDK_VERSION)) {
            if (awsVolume.state().equals(VolumeState.IN_USE) && awsVolume.volumeType().equals(VolumeType.GP3)) {
                return MigrationState.COMPLETE;
            } else {
                // Since the state is definitely not FAILED and the destired state is not yet met, return IN_PROGRESS
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
            if (sorted.get(i).getDetailedStackStatus().equals(DetailedStackStatus.VOLUME_MIGRATION_COMPLETE) ||
                    sorted.get(i).getDetailedStackStatus().equals(DetailedStackStatus.VOLUME_MIGRATION_FAILED)) {
                // If we encounter an ending state first, then we know the previous process (if any) has finished
                // and a new process should start.
                return false;
            }

            if (sorted.get(i).getDetailedStackStatus().equals(DetailedStackStatus.VOLUME_MIGRATION_IN_PROGRESS)) {
                // If we encounter an starting state first, then we know the stack patch process has started.
                return true;
            }
        }

        // We didn't encounter any relevant states so return false.
        return false;
    }

    /**
     * Builds the status message storing volumes that started migration, completed, and failed.
     * Format: started:vol1,vol2,vol3|failed:vol4,vol5|completed:vol6,vol7
     * 
     * @param startedVolumeIds list of volume IDs that successfully started migration and are still in progress
     * @param failedVolumeIds list of volume IDs that failed (either to initiate or during migration)
     * @param completedVolumeIds list of volume IDs that have completed migration successfully
     * @return the formatted status message string
     */
    private String buildMigrationStateMessage(List<String> startedVolumeIds, List<String> failedVolumeIds, List<String> completedVolumeIds) {
        String startedPart = STARTED_PREFIX + (startedVolumeIds != null ? String.join(",", startedVolumeIds) : "");
        String failedPart = FAILED_PREFIX + (failedVolumeIds != null ? String.join(",", failedVolumeIds) : "");
        String completedPart = COMPLETED_PREFIX + (completedVolumeIds != null ? String.join(",", completedVolumeIds) : "");
        return startedPart + PART_SEPARATOR + failedPart +  PART_SEPARATOR + completedPart;
    }

    /**
     * Parses the status message from the last UPDATE_IN_PROGRESS to extract volumes that started migration,
     * completed migration, and failed migration.
     * 
     * @param statusList the list of stack statuses (will find the most recent UPDATE_IN_PROGRESS)
     * @return MigrationVolumeState with started, failed, and completed volume IDs, or empty lists if not parseable
     */
    private MigrationVolumeState parseMigrationStateFromStatus(List<StackStatus> statusList) {
        Optional<StackStatus> lastUpdateInProgress = findLastUpdateInProgressStatus(statusList);
        if (lastUpdateInProgress.isEmpty()) {
            return new MigrationVolumeState();
        }
        String statusReason = lastUpdateInProgress.get().getStatusReason();
        if (Strings.isNullOrEmpty(statusReason)) {
            return new MigrationVolumeState();
        }
        return parseMigrationStateMessage(statusReason);
    }

    /**
     * Collects all volume IDs from stack statuses for past successful migration runs
     * ({@link DetailedStackStatus#VOLUME_MIGRATION_COMPLETE}).
     *
     * @param stackId the stack id
     * @return distinct volume IDs from started, completed, and failed lists in those status reasons
     */
    private Set<String> parseCompletePastMigrationStateFromStatus(Long stackId) {
        List<StackStatus> statusList = stackStatusService.findAllStackStatusesById(stackId);

        List<StackStatus> completedRuns = statusList.stream()
                .filter(status -> status.getStatus() == Status.AVAILABLE)
                .filter(status -> status.getDetailedStackStatus() == DetailedStackStatus.VOLUME_MIGRATION_COMPLETE)
                .toList();

        Set<String> visited = new HashSet<>();
        for  (StackStatus status : completedRuns) {
            MigrationVolumeState currState = parseMigrationStateMessage(status.getStatusReason());
            visited.addAll(currState.startedVolumeIds());
            visited.addAll(currState.completedVolumeIds());
            visited.addAll(currState.failedVolumeIds());
        }

        return visited;
    }

    /**
     * Parses a status reason string into MigrationVolumeState.
     * Expected format: started:vol1,vol2|failed:vol3|completed:vol4,vol5
     * 
     * @param statusReason the status reason string to parse
     * @return MigrationVolumeState containing lists of started, failed, and completed volume IDs
     */
    private MigrationVolumeState parseMigrationStateMessage(String statusReason) {
        List<String> started = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        List<String> completed = new ArrayList<>();

        if (statusReason.contains(PART_SEPARATOR)) {
            String[] parts = statusReason.split("\\" + PART_SEPARATOR, NUM_STATE_PARTS);
            for (String part : parts) {
                String trimmed = part.trim();
                if (trimmed.startsWith(STARTED_PREFIX)) {
                    started = parseVolumeIds(trimmed, STARTED_PREFIX);
                } else if (trimmed.startsWith(FAILED_PREFIX)) {
                    failed = parseVolumeIds(trimmed, FAILED_PREFIX);
                } else if (trimmed.startsWith(COMPLETED_PREFIX)) {
                    completed = parseVolumeIds(trimmed, COMPLETED_PREFIX);
                }
            }
        }
        return new MigrationVolumeState(started, failed, completed);
    }

    /**
     * Parses a comma-separated list of volume IDs from an input string after removing a prefix.
     * 
     * @param input the input string containing prefix and volume IDs
     * @param prefix the prefix to remove from the input string
     * @return list of trimmed, non-empty volume IDs
     */
    private List<String> parseVolumeIds(String input, String prefix) {
        String ids = input.substring(prefix.length()).trim();
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Finds the most recent StackStatus with UPDATE_IN_PROGRESS and VOLUME_MIGRATION_IN_PROGRESS.
     * 
     * @param statusList the list of stack statuses to search
     * @return the most recent matching StackStatus, or empty if none found
     */
    private Optional<StackStatus> findLastUpdateInProgressStatus(List<StackStatus> statusList) {
        return statusList.stream()
                .filter(status -> status.getStatus() == Status.UPDATE_IN_PROGRESS)
                .filter(status -> status.getDetailedStackStatus() == DetailedStackStatus.VOLUME_MIGRATION_IN_PROGRESS)
                .max(Comparator.comparing(StackStatus::getCreated));
    }

    /**
     * Returns the latest volume modification state for a given volume ID by querying AWS.
     * 
     * @param volumeId the ID of the volume to check
     * @param awsClient the AWS EC2 client to use
     * @return the latest volume modification state, or empty if no modification state is found
     */
    private Optional<VolumeModification> getLatestVolumeModification(String volumeId, AmazonEc2Client awsClient) {
        DescribeVolumesModificationsRequest request = DescribeVolumesModificationsRequest.builder()
                .volumeIds(volumeId)
                .build();

        DescribeVolumesModificationsResponse response = awsClient.describeVolumeModifications(request);
        if (response.volumesModifications().isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(response.volumesModifications().getFirst());
    }

    /**
     * Returns a list of GP2 volumes that are valid and exist in AWS. The volumes are filtered to only include volumes that:
     * - Have a valid ID
     * - Have a valid size
     * - Exist in AWS
     * - Are GP2 in AWS
     * - Are attached to a VM (IN_USE state)
     * <p/>
     * This method is bounded to account for large clusters. The maximum number of volumes returned is
     * limited by the VOLUME_BATCH_LIMIT constant.
     * 
     * @param stack the stack to get the GP2 volumes from
     * @param awsClient the AWS EC2 client to use for querying volume state
     * @return a list of GP2 volumes that are valid and exist in AWS, or an empty list if no valid volumes are found
     */
    private List<VolumeSetAttributes.Volume> getValidatedGp2VolumeList(Stack stack, AmazonEc2Client awsClient) {
        // Get a list of gp2 volumes that Cloudbreak knows about.
        Collection<Resource> resources = resourceService.findAllByStackIdAndResourceTypeIn(stack.getId(),
                List.of(ResourceType.AWS_ROOT_DISK, ResourceType.AWS_VOLUMESET));
        List<VolumeSetAttributes.Volume> cloudbreakGp2Volumes = stackUtil.getGp2VolumesFromResources(resources);

        if (cloudbreakGp2Volumes.size() > VOLUME_BATCH_LIMIT) {
            // We want to limit the number of volume we process at any given point in time. Otherwise, there is the possibility
            // of getting throttled by AWS or providing too many filter values to the API and getting errors.
            cloudbreakGp2Volumes = cloudbreakGp2Volumes.subList(0, VOLUME_BATCH_LIMIT);
            LOGGER.warn("Found more than {} GP2 volumes to migrate for stack {}, limiting to {} volumes",
                    VOLUME_BATCH_LIMIT, stack.getResourceCrn(), VOLUME_BATCH_LIMIT);
        }

        // Based on the stack information, get a map of volume IDs to AWS volume state.
        Map<String, Volume> awsVolumeMap = getAwsGp2Volumes(stack, awsClient, cloudbreakGp2Volumes);

        // Filter only volumes that actually exist in AWS and are GP2
        List<VolumeSetAttributes.Volume> validGp2Volumes = cloudbreakGp2Volumes.stream()
                .filter(volume -> {
                    if (volume.getId() == null || volume.getId().isEmpty()) {
                        // Volume ID is empty, don't include since we need the ID later.
                        return false;
                    }
                    if (volume.getSize() == null || volume.getSize() <= 0) {
                        // Volume size is empty, don't include since we need the size later.
                        return false;
                    }
                    Volume awsVolume = awsVolumeMap.get(volume.getId());
                    if (awsVolume == null) {
                        // The volume is unknown to AWS, don't include.
                        return false;
                    }
                    if (VolumeType.GP2 != awsVolume.volumeType()) {
                        // The volume is not GP2, don't include.
                        return false;
                    }
                    if (VolumeState.IN_USE != awsVolume.state()) {
                        // The volume state is not attached to a VM so we'll skip it.
                        // See https://docs.aws.amazon.com/ebs/latest/userguide/ebs-describing-volumes.html#volume-state for more information.
                        // We don't want to migrate volumes that are not attached to a VM (e.g. AVAILABLE) or volumes that have
                        // a state of ERROR, CREATING, etc.
                        return false;
                    }
                    return true;
                })
                .toList();

        return validGp2Volumes;
    }

    /**
     * Initiates the AWS volume conversion from GP2 to GP3 for a stack.
     * This method validates GP2 volumes, calculates appropriate IOPS for GP3 conversion,
     * and initiates the AWS volume modification for each volume. Volumes are processed
     * individually to avoid AWS API throttling.
     * 
     * @param stack the stack to initiate the volume conversion for
     * @param awsClient the AWS EC2 client used to perform volume modifications
     * @param volumeIdsToProcess optional list of specific volumes to process; if null or empty,
     *                           all valid GP2 volumes will be retrieved and processed
     * @return a ConversionStatus object containing the status of the volume conversion.
     *         The object contains a map of volume IDs to CloudbreakException for failed conversions
     *         and a list of volume IDs that were successfully initiated. If no volumes were migrated,
     *         the object will contain an empty map and list.
     */
    private ConversionStatus initiateAwsVolumeConversionForStack(Stack stack, AmazonEc2Client awsClient, List<VolumeSetAttributes.Volume> volumeIdsToProcess) {
        List<VolumeSetAttributes.Volume> validGp2Volumes = volumeIdsToProcess;
        if ((volumeIdsToProcess == null) || volumeIdsToProcess.isEmpty()) {
            validGp2Volumes = getValidatedGp2VolumeList(stack, awsClient);
        }

        if (validGp2Volumes.isEmpty()) {
            LOGGER.info("No valid GP2 volumes to migrate for stack {} after validation", stack.getResourceCrn());
            return new ConversionStatus(new HashMap<>(), new ArrayList<>());
        }

        // Calculate IOPS for each volume based on its size. We have already filtered out missing IDs and Size, so
        // we don't need to recheck the values again.
        Map<String, Integer> volumeIopsMap = validGp2Volumes.stream()
                .collect(Collectors.toMap(
                        VolumeSetAttributes.Volume::getId,
                        volume -> {
                            int targetIops = volumeIopsCalculator.getEquivalentGp3IopsforGp2Volume(volume.getSize());
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
                String message = format("Migration AWS error for volume %s : %s", stack.getResourceCrn(), volumeId);
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
     * Updates the database resource metadata to reflect a migrated GP3 volume.
     * After AWS volume migration is initiated, this method updates the stored VolumeSetAttributes
     * to show GP3 as the volume type with the specified IOPS, ensuring database state matches AWS reality.
     * 
     * @param stackId the ID of the stack whose resources should be updated
     * @param volumeId the ID of the volume that was migrated
     */
    private void updateResourceMetadata(Long stackId, String volumeId) {
        Collection<Resource> resources = resourceService.getAllByStackId(stackId);
        LOGGER.debug("Updating database metadata for volume {}", volumeId);

        boolean found = false;
        for (Resource res : resources) {
            Optional<VolumeSetAttributes> volumeSetOptional = resourceAttributeUtil.getTypedAttributes(res, VolumeSetAttributes.class);
            if (volumeSetOptional.isPresent()) {
                VolumeSetAttributes volumeSet = volumeSetOptional.get();
                List<VolumeSetAttributes.Volume> existingVolumes = volumeSet.getVolumes();
                if (existingVolumes != null) {
                    for (VolumeSetAttributes.Volume volume : existingVolumes) {
                        if (volumeId.equals(volume.getId())) {
                            volume.setType(VolumeType.GP3.toString());
                            res.setAttributes(new Json(volumeSet));
                            resourceService.save(res);
                            LOGGER.info("Successfully updated volume {} to GP3", volumeId);
                            found = true;
                            break;
                        }
                    }
                }
                if (found) {
                    break;
                }
            }
        }

        if (!found) {
            LOGGER.warn("Volume {} not found in resources for stack {}", volumeId, stackId);
        }
    }

    /**
     * Queries AWS EC2 API to retrieve volume information for the specified Cloudbreak volumes.
     * Returns a map of volume IDs to their corresponding AWS Volume objects for quick lookup.
     * 
     * @param stack the stack to retrieve volumes from
     * @param awsClient the AWS EC2 client to use for querying
     * @param cloudbreakGp2Volumes the list of Cloudbreak volumes to query in AWS
     * @return a map of volume ID to AWS Volume object
     */
    private Map<String, Volume> getAwsGp2Volumes(Stack stack, AmazonEc2Client awsClient, List<VolumeSetAttributes.Volume> cloudbreakGp2Volumes) {
        if (cloudbreakGp2Volumes.isEmpty()) {
            return Map.of();
        }

        List<String> volumeIds = cloudbreakGp2Volumes.stream()
                .map(VolumeSetAttributes.Volume::getId)
                .toList();

        DescribeVolumesRequest request = DescribeVolumesRequest.builder()
                .volumeIds(volumeIds)
                .build();

        DescribeVolumesResponse response = awsClient.describeVolumes(request);
        return response.volumes().stream()
                .collect(Collectors.toMap(Volume::volumeId, Function.identity()));
    }

    /**
     * Queries AWS EC2 API to retrieve information for a specific volume.
     * 
     * @param volumeId the ID of the volume to query
     * @param awsClient the AWS EC2 client to use for querying
     * @return the AWS Volume object if found, or empty if the volume doesn't exist
     */
    private Optional<Volume> getAwsVolumeInfo(String volumeId, AmazonEc2Client awsClient) {
        LOGGER.debug("Querying volume {}", volumeId);

        DescribeVolumesRequest describeVolumeRequest = DescribeVolumesRequest.builder()
                .volumeIds(volumeId)
                .build();
        DescribeVolumesResponse response = awsClient.describeVolumes(describeVolumeRequest);
        if (response.volumes().isEmpty()) {
            return Optional.empty();
        } else {
            Volume volume = response.volumes().get(0);
            return Optional.of(volume);
        }
    }

    /**
     * Creates and returns an AWS EC2 client for the given stack.
     * 
     * @param stack the stack to create the client for
     * @return the authenticated AWS EC2 client
     */
    @VisibleForTesting
    AmazonEc2Client getAwsClient(Stack stack) {
        CloudConnectResources ccr = cloudConnectorHelper.getCloudConnectorResources(stack);
        AuthenticatedContext authContext = ccr.getAuthenticatedContext();
        return new AuthenticatedContextView(authContext).getAmazonEC2Client();
    }

    /**
     * Holds the parsed migration state from stack status. This is only used internally to allow for simplier maintenance of state.
     */
    private static class MigrationVolumeState {
        private List<String> startedVolumeIds;

        private List<String> failedVolumeIds;

        private List<String> completedVolumeIds;

        private MigrationVolumeState() {
            this.startedVolumeIds = new ArrayList<>();
            this.failedVolumeIds = new ArrayList<>();
            this.completedVolumeIds = new ArrayList<>();
        }

        private MigrationVolumeState(List<String> startedVolumeIds, List<String> failedVolumeIds, List<String> completedVolumeIds) {
            this.startedVolumeIds = startedVolumeIds;
            this.failedVolumeIds = failedVolumeIds;
            this.completedVolumeIds = completedVolumeIds;
        }

        public List<String> startedVolumeIds() {
            return startedVolumeIds;
        }

        public List<String> failedVolumeIds() {
            return failedVolumeIds;
        }

        public List<String> completedVolumeIds() {
            return completedVolumeIds;
        }
    }
}
