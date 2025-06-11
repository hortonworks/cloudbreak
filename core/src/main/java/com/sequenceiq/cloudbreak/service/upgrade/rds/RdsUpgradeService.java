package com.sequenceiq.cloudbreak.service.upgrade.rds;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_RDS_UPGRADE_ALREADY_UPGRADED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_RDS_UPGRADE_NOT_AVAILABLE;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.DatabaseServerStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.StackDatabaseServerResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.RdsUpgradeV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.database.MajorVersion;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.projection.StackListItem;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.database.DatabaseService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class RdsUpgradeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RdsUpgradeService.class);

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private StackService stackService;

    @Inject
    private ReactorFlowManager reactorFlowManager;

    @Inject
    private DatabaseService databaseService;

    @Inject
    private CloudbreakMessagesService messagesService;

    @Inject
    private EnvironmentService environmentService;

    @Value("${cb.db.env.upgrade.rds.targetversion}")
    private TargetMajorVersion defaultTargetMajorVersion;

    @Inject
    private DatabaseUpgradeRuntimeValidator databaseUpgradeRuntimeValidator;

    @Inject
    private EntitlementService entitlementService;

    public void checkUpgradeRds(NameOrCrn nameOrCrn, TargetMajorVersion targetMajorVersion) {
        TargetMajorVersion calculatedVersion = ObjectUtils.defaultIfNull(targetMajorVersion, defaultTargetMajorVersion);
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        StackView stack = stackDtoService.getStackViewByNameOrCrn(nameOrCrn, accountId);
        MDCBuilder.buildMdcContext(stack);
        LOGGER.info("Checking if RDS upgrade is possible for stack {} to version {}, request version was {}",
                nameOrCrn.getNameOrCrn(), calculatedVersion, targetMajorVersion);
        validateAttachedDatahubsAreNotRunning(stack, accountId);
    }

    public RdsUpgradeV4Response upgradeRds(NameOrCrn nameOrCrn, TargetMajorVersion targetMajorVersion, boolean forced) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        StackDto stackDto = stackDtoService.getByNameOrCrn(nameOrCrn, accountId);
        StackView stackView = stackDto.getStack();
        MDCBuilder.buildMdcContext(stackView);
        TargetMajorVersion calculatedVersion = getTargetMajorVersion(targetMajorVersion, stackDto.getCloudPlatform());

        boolean dataHubWithEmbeddedDatabase = !stackView.isDatalake() && stackDto.getExternalDatabaseCreationType().isEmbedded();
        if (dataHubWithEmbeddedDatabase) {
            LOGGER.warn("Database upgrade is not allowed for DataHubs with embedded database ");
            throw new BadRequestException("Database upgrade is not allowed for DataHubs with embedded database");
        } else {
            LOGGER.info("RDS upgrade has been initiated for stack {} to version {}, request version was {}",
                    nameOrCrn.getNameOrCrn(), calculatedVersion, targetMajorVersion);
            validate(nameOrCrn, stackView, calculatedVersion, accountId, forced);
            LOGGER.info("External database for stack {} will be upgraded to version {}", stackView.getName(), calculatedVersion.getMajorVersion());
            DetailedEnvironmentResponse environment = environmentService.getByCrn(stackView.getEnvironmentCrn());
            String backupLocation = getBackupLocation(environment);
            String backupInstanceProfile = getBackupInstanceProfile(environment);
            return triggerRdsUpgradeFlow(stackView, calculatedVersion, backupLocation, backupInstanceProfile);
        }
    }

    private TargetMajorVersion getTargetMajorVersion(TargetMajorVersion requestedTargetVersion, String cloudPlatform) {
        TargetMajorVersion calculatedVersion = ObjectUtils.defaultIfNull(requestedTargetVersion, defaultTargetMajorVersion);
        LOGGER.debug("Calculated upgrade target is {}, based on requested {}, general default {}",
                calculatedVersion,
                requestedTargetVersion,
                defaultTargetMajorVersion);
        return calculatedVersion;
    }

    private void validate(NameOrCrn nameOrCrn, StackView stack, TargetMajorVersion targetMajorVersion, String accountId, boolean forced) {
        StackDatabaseServerResponse databaseServer = databaseService.getDatabaseServer(nameOrCrn, accountId);
        validateRdsIsNotUpgraded(databaseServer, targetMajorVersion, forced);
        validateRuntimeEligibleForUpgrade(stack, targetMajorVersion.getMajorVersion(), accountId);
        validateEngineVersionEligibleForUpgrade(stack, databaseServer, targetMajorVersion.getMajorVersion());
        validateStackStatus(stack);
        validateAttachedDatahubsAreNotRunning(stack, accountId);
        validateRdsIsAvailableForUpgrade(databaseServer);
        validatePostgresServiceStopDisabledForAzure(stack, accountId);
    }

    private void validatePostgresServiceStopDisabledForAzure(StackView stack, String accountId) {
        if (CloudPlatform.AZURE.name().equalsIgnoreCase(stack.getCloudPlatform())
                && entitlementService.isPostgresUpgradeSkipServicesAndCmStopEnabled(accountId)) {
            throw new BadRequestException("Azure external database cannot be upgraded if 'CDP_POSTGRES_UPGRADE_SKIP_SERVICE_STOP' entitlement is enabled");
        }
    }

    private void validateRdsIsNotUpgraded(StackDatabaseServerResponse databaseServer, TargetMajorVersion targetMajorVersion, boolean forced) {
        String currentRdsVersion = getCurrentRdsVersion(databaseServer);
        LOGGER.debug("Current version: [{}] Target version: [{}] Forced: [{}]", currentRdsVersion, targetMajorVersion.getMajorVersion(), forced);
        if (currentRdsVersion.equals(targetMajorVersion.getMajorVersion()) && !forced) {
            alreadyOnLatestAnswer(targetMajorVersion);
        }
    }

    private String getCurrentRdsVersion(StackDatabaseServerResponse databaseServer) {
        return Optional.ofNullable(databaseServer)
                .map(StackDatabaseServerResponse::getMajorVersion)
                .map(MajorVersion::getMajorVersion)
                .orElse(MajorVersion.VERSION_10.getMajorVersion());
    }

    private void alreadyOnLatestAnswer(TargetMajorVersion targetMajorVersion) {
        String message = getMessage(CLUSTER_RDS_UPGRADE_ALREADY_UPGRADED, List.of(targetMajorVersion.getMajorVersion()));
        throw new BadRequestException(message);
    }

    private void validateRuntimeEligibleForUpgrade(StackView stack, String targetMajorVersion, String accountId) {
        Optional<String> runtimeValidationError =
                databaseUpgradeRuntimeValidator.validateRuntimeVersionForUpgrade(stack.getStackVersion(), targetMajorVersion, accountId);
        if (runtimeValidationError.isPresent()) {
            LOGGER.warn("There was a validation error: {}", runtimeValidationError.get());
            throw new BadRequestException(runtimeValidationError.get());
        }
    }

    private void validateEngineVersionEligibleForUpgrade(StackView stack, StackDatabaseServerResponse databaseServer, String majorVersion) {
        String currentRdsVersion = getCurrentRdsVersion(databaseServer);
        Optional<String> majorVersionAvailabilityError = databaseUpgradeRuntimeValidator.validateTargetMajorVersionAvailability(
                majorVersion,
                currentRdsVersion,
                stack);
        if (majorVersionAvailabilityError.isPresent()) {
            LOGGER.warn("There was a validation error: {}", majorVersionAvailabilityError.get());
            throw new BadRequestException(majorVersionAvailabilityError.get());
        }
    }

    private void validateStackStatus(StackView stack) {
        Status stackStatus = stack.getStatus();
        if (!stackStatus.isAvailable() && Status.EXTERNAL_DATABASE_UPGRADE_FAILED != stackStatus) {
            LOGGER.warn("Stack {} is not available for RDS upgrade", stack.getName());
            throw new BadRequestException(getMessage(CLUSTER_RDS_UPGRADE_NOT_AVAILABLE, List.of(stackStatus.name())));
        }
    }

    private void validateAttachedDatahubsAreNotRunning(StackView stack, String accountId) {
        if (!entitlementService.isPostgresUpgradeAttachedDatahubsCheckSkipped(accountId) && stack.isDatalake()) {
            Set<StackListItem> datahubs = stackService.getByWorkspaceId(restRequestThreadLocalService.getRequestedWorkspaceId(), stack.getEnvironmentCrn(),
                    List.of(StackType.WORKLOAD));
            String notStoppedAttachedClusters = datahubs.stream()
                    .filter(datahub -> !Status.getAllowedDataHubStatesForSdxUpgrade().contains(datahub.getStackStatus())
                            || !Status.getAllowedDataHubStatesForSdxUpgrade().contains(datahub.getClusterStatus()))
                    .map(StackListItem::getName)
                    .collect(Collectors.joining(","));
            if (StringUtils.isNotEmpty(notStoppedAttachedClusters)) {
                String msg = String.format("There are attached Data Hub clusters in incorrect state: %s. "
                        + "Please stop those to be able to perform the database server upgrade.", notStoppedAttachedClusters);
                LOGGER.warn(msg);
                throw new BadRequestException(msg);
            }
        }
    }

    private void validateRdsIsAvailableForUpgrade(StackDatabaseServerResponse databaseServer) {
        DatabaseServerStatus dbStatus = databaseServer.getStatus();
        if (dbStatus == null || !dbStatus.isAvailableForUpgrade()) {
            String msg = "Upgrading database server is not possible as database server is not available";
            msg += dbStatus == null ? "." : String.format(", it is in %s state.", dbStatus.toString());
            LOGGER.warn(msg);
            throw new BadRequestException(msg);
        }
    }

    private String getBackupLocation(DetailedEnvironmentResponse environment) {
        String backupStorageLocation = environment.getBackupLocation();
        boolean hasBackupLocation = Objects.nonNull(backupStorageLocation);
        LOGGER.debug("Backup location for CRN {} has {} been found {}",
                environment.getCrn(),
                hasBackupLocation ? "" : "NOT",
                hasBackupLocation ? backupStorageLocation : "");
        return backupStorageLocation;
    }

    private String getBackupInstanceProfile(DetailedEnvironmentResponse environment) {
        String backupInstanceProfile = environment.getBackupInstanceProfile();
        boolean hasBackupInstanceProfile = Objects.nonNull(backupInstanceProfile);
        LOGGER.debug("Backup instance profile for CRN {} has {} been found {}",
                environment.getCrn(),
                hasBackupInstanceProfile ? "" : "NOT",
                hasBackupInstanceProfile ? backupInstanceProfile : "");
        return backupInstanceProfile;
    }

    private RdsUpgradeV4Response triggerRdsUpgradeFlow(StackView stack, TargetMajorVersion targetMajorVersion, String backupLocation,
            String backupInstanceProfile) {
        FlowIdentifier triggeredFlowId = reactorFlowManager.triggerRdsUpgrade(stack.getId(), targetMajorVersion, backupLocation, backupInstanceProfile);
        return new RdsUpgradeV4Response(triggeredFlowId, targetMajorVersion);
    }

    private String getMessage(ResourceEvent resourceEvent, List<String> args) {
        return messagesService.getMessage(resourceEvent.getMessage(), args);
    }
}
