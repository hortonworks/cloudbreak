package com.sequenceiq.distrox.v1.distrox.service.upgrade.rds;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.StackDatabaseServerResponse;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnParseException;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.common.database.MajorVersion;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.database.DatabaseDefaultVersionProvider;
import com.sequenceiq.cloudbreak.service.database.DatabaseService;
import com.sequenceiq.cloudbreak.service.database.DbOverrideConfig;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.util.MajorVersionComparator;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.rds.DistroXDatabaseUpgradeStatus;

@Service
public class DistroXRdsUpgradeStatusService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXRdsUpgradeStatusService.class);

    private static final int MAX_BATCH_SIZE = 50;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private DatabaseService databaseService;

    @Inject
    private DatabaseDefaultVersionProvider databaseDefaultVersionProvider;

    @Inject
    private DbOverrideConfig dbOverrideConfig;

    public List<DistroXDatabaseUpgradeStatus> getUpgradeRequiredByDatahubCrns(List<String> datahubCrns) {
        if (datahubCrns == null) {
            throw new BadRequestException("Datahub CRN list must not be null.");
        }
        if (datahubCrns.size() > MAX_BATCH_SIZE) {
            throw new BadRequestException(String.format("Datahub CRN list must not exceed %d entries.", MAX_BATCH_SIZE));
        }
        return datahubCrns.stream()
                .map(this::resolveUpgradeStatusByCrn)
                .toList();
    }

    public DistroXDatabaseUpgradeStatus getUpgradeRequired(NameOrCrn nameOrCrn) {
        if (nameOrCrn.hasCrn()) {
            validateDatahubCrn(nameOrCrn.getCrn());
        }
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        try {
            StackDto stackDto = stackDtoService.getByNameOrCrn(nameOrCrn, accountId);
            return resolveUpgradeStatusForStack(stackDto);
        } catch (NotFoundException e) {
            String identifier = nameOrCrn.getNameOrCrn();
            LOGGER.warn("Datahub not found for '{}': {}", identifier, e.getMessage());
            throw NotFoundException.notFoundException("Datahub", identifier);
        } catch (Exception e) {
            String identifier = nameOrCrn.getNameOrCrn();
            LOGGER.warn("Failed to resolve datahub '{}', defaulting to UNKNOWN: {}", identifier, e.getMessage());
            return DistroXDatabaseUpgradeStatus.unknown(nameOrCrn.hasCrn() ? nameOrCrn.getCrn() : null);
        }
    }

    private DistroXDatabaseUpgradeStatus resolveUpgradeStatusByCrn(String datahubCrn) {
        validateDatahubCrn(datahubCrn);
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        try {
            StackDto stackDto = stackDtoService.getByNameOrCrn(NameOrCrn.ofCrn(datahubCrn), accountId);
            return resolveUpgradeStatusForStack(stackDto);
        } catch (NotFoundException e) {
            LOGGER.warn("Datahub not found for CRN '{}': {}", datahubCrn, e.getMessage());
            return DistroXDatabaseUpgradeStatus.noDatahub(datahubCrn);
        } catch (Exception e) {
            LOGGER.warn("Failed to check upgrade status for datahub CRN '{}', defaulting to UPGRADE_NOT_REQUIRED (fail-open): {}",
                    datahubCrn, e.getMessage());
            return DistroXDatabaseUpgradeStatus.unknown(datahubCrn);
        }
    }

    private DistroXDatabaseUpgradeStatus resolveUpgradeStatusForStack(StackDto stackDto) {
        String datahubCrn = stackDto.getStack().getResourceCrn();
        try {
            if (stackDto.getExternalDatabaseCreationType().isEmbedded()) {
                LOGGER.debug("Datahub {} uses embedded database; upgrade not applicable", datahubCrn);
                return DistroXDatabaseUpgradeStatus.upgradeNotRequired(datahubCrn, null);
            }
            String accountId = ThreadBasedUserCrnProvider.getAccountId();
            String targetVersionStr = databaseDefaultVersionProvider.calculateDbVersionBasedOnRuntime(stackDto.getStack().getStackVersion(), null);
            TargetMajorVersion targetMajorVersion = TargetMajorVersion.fromVersion(targetVersionStr);
            StackDatabaseServerResponse databaseResponse = databaseService.getDatabaseServer(NameOrCrn.ofCrn(datahubCrn), accountId);
            MajorVersion currentMajorVersion = databaseResponse.getMajorVersion();
            String currentVersionStr = currentMajorVersion != null ? currentMajorVersion.getMajorVersion() : null;
            boolean upgradeNeeded = isUpgradeNeeded(currentMajorVersion, targetMajorVersion);
            DistroXDatabaseUpgradeStatus status;
            if (upgradeNeeded) {
                status = DistroXDatabaseUpgradeStatus.upgradeRequired(datahubCrn, targetVersionStr, currentVersionStr);
            } else {
                status = DistroXDatabaseUpgradeStatus.upgradeNotRequired(datahubCrn, currentVersionStr);
            }
            if (currentVersionStr != null) {
                dbOverrideConfig.getEolDate(currentVersionStr)
                        .filter(eol -> !LocalDate.now().isBefore(eol))
                        .ifPresent(eol -> status.setEolDate(eol.toString()));
            }
            return status;
        } catch (Exception e) {
            LOGGER.warn("Failed to check RDS upgrade status for datahub '{}', defaulting to UPGRADE_NOT_REQUIRED (fail-open): {}",
                    datahubCrn, e.getMessage());
            return DistroXDatabaseUpgradeStatus.unknown(datahubCrn);
        }
    }

    private void validateDatahubCrn(String datahubCrn) {
        if (datahubCrn == null) {
            throw new BadRequestException("Datahub CRN must not be null.");
        }
        try {
            Crn parsedCrn = Crn.safeFromString(datahubCrn);
            if (!CrnResourceDescriptor.DATAHUB.checkIfCrnMatches(parsedCrn)) {
                throw new BadRequestException(String.format("Invalid Datahub CRN provided: %s", datahubCrn));
            }
        } catch (IllegalArgumentException | CrnParseException e) {
            throw new BadRequestException(String.format("Invalid Datahub CRN provided: %s", datahubCrn));
        }
    }

    private boolean isUpgradeNeeded(MajorVersion currentMajorVersion, TargetMajorVersion targetMajorVersion) {
        if (Objects.isNull(currentMajorVersion)) {
            return true;
        }
        MajorVersionComparator majorVersionComparator = new MajorVersionComparator();
        return majorVersionComparator.compare(currentMajorVersion, targetMajorVersion.convertToMajorVersion()) < 0;
    }
}
