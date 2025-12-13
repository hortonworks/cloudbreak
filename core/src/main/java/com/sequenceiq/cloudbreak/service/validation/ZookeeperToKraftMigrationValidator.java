package com.sequenceiq.cloudbreak.service.validation;

import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_ENABLE_ZOOKEEPER_TO_KRAFT_MIGRATION;

import java.util.Comparator;
import java.util.Set;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cluster.status.KraftMigrationStatus;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.util.VersionComparator;

@Component
public class ZookeeperToKraftMigrationValidator {

    private static final String ZOOKEEPER_TO_KRAFT_MIGRATION_MIN_VERSION = "7.3.2";

    @Value("${cb.cm.zookeeperToKraftMigration.supportedTemplates}")
    private Set<String> kraftMigrationSupportedTemplates;

    @Inject
    private EntitlementService entitlementService;

    public void validateZookeeperToKraftMigrationState(String kraftMigrationState) {
        if (KraftMigrationStatus.BROKERS_IN_KRAFT.name().equals(kraftMigrationState)
                || KraftMigrationStatus.KRAFT_INSTALLED.name().equals(kraftMigrationState)) {
            throw new BadRequestException("Cannot start KRaft migration. The cluster has been migrated already to KRaft.");
        }

        if (!KraftMigrationStatus.ZOOKEEPER_INSTALLED.name().equals(kraftMigrationState)
        && !KraftMigrationStatus.PRE_MIGRATION.name().equals(kraftMigrationState)) {
            throw new BadRequestException(String.format("Cannot start KRaft migration. The cluster is being migrated to KRaft and has the status: %s.",
                    kraftMigrationState));
        }
    }

    public void validateZookeeperToKraftMigrationStateForFinalization(String kraftMigrationState) {
        if (KraftMigrationStatus.KRAFT_INSTALLED.name().equals(kraftMigrationState)) {
            throw new BadRequestException("Cannot finalize KRaft migration. KRaft migration is already finalized for this cluster.");
        }

        if (!KraftMigrationStatus.BROKERS_IN_KRAFT.name().equals(kraftMigrationState)) {
            throw new BadRequestException("Cannot finalize KRaft migration. The cluster has not been migrated to KRaft yet.");
        }
    }

    public void validateZookeeperToKraftMigrationStateForRollback(String kraftMigrationState) {
        if (KraftMigrationStatus.KRAFT_INSTALLED.name().equals(kraftMigrationState)) {
            throw new BadRequestException("Cannot rollback KRaft migration. KRaft migration is already finalized for this cluster.");
        }

        if (!KraftMigrationStatus.BROKERS_IN_KRAFT.name().equals(kraftMigrationState)) {
            throw new BadRequestException("Cannot rollback KRaft migration. The cluster has not been migrated to KRaft yet.");
        }
    }

    public boolean isMigrationFromZookeeperToKraftSupported(StackDto stack, String accountId) {
        boolean clusterAvailable = stack.getStatus().isAvailable();
        boolean kraftMigrationEntitlementEnabled = entitlementService.isZookeeperToKRaftMigrationEnabled(accountId);

        return clusterAvailable && hasStreamsMessagingTemplateType(stack) && isZookeeperToKRaftMigrationSupportedForStackVersion(stack.getStackVersion())
                && kraftMigrationEntitlementEnabled;
    }

    public void validateZookeeperToKraftMigrationEligibility(StackDto stack, String accountId) {
        if (!stack.getStatus().isAvailable()) {
            throw new BadRequestException("Zookeeper to KRaft migration can only be performed when the cluster is in Available state. " +
                    "Please ensure the cluster is fully operational before starting the migration.");
        }

        if (!hasStreamsMessagingTemplateType(stack)) {
            throw new BadRequestException("Zookeeper to KRaft migration is supported only for the following template types: "
                    + String.join(", ", kraftMigrationSupportedTemplates));
        }

        if (!isZookeeperToKRaftMigrationSupportedForStackVersion(stack.getStackVersion())) {
            throw new BadRequestException("Zookeeper to KRaft migration is supported only for CDP version " + ZOOKEEPER_TO_KRAFT_MIGRATION_MIN_VERSION);
        }

        if (!entitlementService.isZookeeperToKRaftMigrationEnabled(accountId)) {
            throw new BadRequestException(String.format("Your account is not entitled to perform Zookeeper to KRaft migration. Please contact Cloudera " +
                    "to enable '%s' entitlement for your account.", CDP_ENABLE_ZOOKEEPER_TO_KRAFT_MIGRATION));
        }
    }

    private boolean isZookeeperToKRaftMigrationSupportedForStackVersion(String version) {
        Comparator<Versioned> versionComparator = new VersionComparator();
        return versionComparator.compare(() -> version, () -> ZOOKEEPER_TO_KRAFT_MIGRATION_MIN_VERSION) == 0;
    }

    private boolean hasStreamsMessagingTemplateType(StackDto stack) {
        if (stack.getBlueprint() == null || stack.getBlueprint().getName() == null) {
            return false;
        }

        String blueprintName = stack.getBlueprint().getName();

        return kraftMigrationSupportedTemplates.stream().anyMatch(blueprintName::contains);
    }
}