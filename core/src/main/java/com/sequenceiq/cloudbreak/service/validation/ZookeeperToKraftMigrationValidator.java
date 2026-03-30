package com.sequenceiq.cloudbreak.service.validation;

import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_ENABLE_ZOOKEEPER_TO_KRAFT_MIGRATION;
import static com.sequenceiq.cloudbreak.cluster.status.KraftMigrationStatus.BROKERS_IN_MIGRATION;
import static com.sequenceiq.cloudbreak.cluster.status.KraftMigrationStatus.NOT_APPLICABLE;
import static com.sequenceiq.cloudbreak.cluster.status.KraftMigrationStatus.PRE_MIGRATION;
import static com.sequenceiq.cloudbreak.cluster.status.KraftMigrationStatus.ZOOKEEPER_INSTALLED;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cluster.status.KraftMigrationStatus;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.util.VersionComparator;

@Component
public class ZookeeperToKraftMigrationValidator {

    private static final String ZOOKEEPER_TO_KRAFT_MIGRATION_MIN_VERSION = "7.3.2";

    private static final Set<KraftMigrationStatus> INVALID_KRAFT_MIGRATION_STATUSES = Set.of(BROKERS_IN_MIGRATION, NOT_APPLICABLE);

    private static final Set<KraftMigrationStatus> INVALID_KRAFT_MIGRATION_FINALIZATION_STATUSES = Set.of(ZOOKEEPER_INSTALLED, PRE_MIGRATION,
            BROKERS_IN_MIGRATION, NOT_APPLICABLE);

    private final EntitlementService entitlementService;

    private final BlueprintService blueprintService;

    public ZookeeperToKraftMigrationValidator(EntitlementService entitlementService, BlueprintService blueprintService) {
        this.entitlementService = entitlementService;
        this.blueprintService = blueprintService;
    }

    public void validateZookeeperToKraftMigrationState(KraftMigrationStatus kraftMigrationState) {
        if (INVALID_KRAFT_MIGRATION_STATUSES.contains(kraftMigrationState)) {
            throw new BadRequestException(String.format("Cannot start KRaft migration. The cluster has [%s] KRaft migration status.", kraftMigrationState));
        }
    }

    public void validateZookeeperToKraftMigrationStateForFinalization(KraftMigrationStatus kraftMigrationState) {
        if (INVALID_KRAFT_MIGRATION_FINALIZATION_STATUSES.contains(kraftMigrationState)) {
            throw new BadRequestException(String.format("Cannot finalize KRaft migration. The cluster has [%s] KRaft migration status.", kraftMigrationState));
        }
    }

    public void validateZookeeperToKraftMigrationEligibility(StackDto stack, String accountId) {
        if (!stack.getStatus().isAvailable()) {
            throw new BadRequestException("Zookeeper to KRaft migration can only be performed when the cluster is in Available state. " +
                    "Please ensure the cluster is fully operational before starting the migration.");
        }

        if (!isKafkaServicePresent(stack)) {
            throw new BadRequestException("Zookeeper to KRaft migration is supported only for templates where Kafka is present.");
        }

        if (!isZookeeperToKRaftMigrationSupportedForStackVersion(stack.getStackVersion())) {
            throw new BadRequestException("Zookeeper to KRaft migration is supported only for CDP version " + ZOOKEEPER_TO_KRAFT_MIGRATION_MIN_VERSION);
        }

        if (!entitlementService.isZookeeperToKRaftMigrationEnabled(accountId)) {
            throw new BadRequestException(String.format("Your account is not entitled to perform Zookeeper to KRaft migration. Please contact Cloudera " +
                    "to enable '%s' entitlement for your account.", CDP_ENABLE_ZOOKEEPER_TO_KRAFT_MIGRATION));
        }
    }

    public boolean isMigrationFromZookeeperToKraftSupported(StackDto stack, String accountId) {
        boolean kraftMigrationEntitlementEnabled = entitlementService.isZookeeperToKRaftMigrationEnabled(accountId);
        return isKafkaServicePresent(stack) && isZookeeperToKRaftMigrationSupportedForStackVersion(stack.getStackVersion())
                && kraftMigrationEntitlementEnabled;
    }

    private boolean isZookeeperToKRaftMigrationSupportedForStackVersion(String version) {
        Comparator<Versioned> versionComparator = new VersionComparator();
        return versionComparator.compare(() -> version, () -> ZOOKEEPER_TO_KRAFT_MIGRATION_MIN_VERSION) == 0;
    }

    private boolean isKafkaServicePresent(StackDto stack) {
        Predicate<Blueprint> bpPredicate =
                bp -> blueprintService.anyOfTheServiceTypesPresentOnBlueprint(bp.getBlueprintJsonText(), List.of("KAFKA"));
        return Optional.ofNullable(stack.getBlueprint())
                .filter(bpPredicate)
                .isPresent();
    }

}