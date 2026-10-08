package com.sequenceiq.cloudbreak.service.validation;

import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_ENABLE_ZOOKEEPER_TO_KRAFT_MIGRATION;
import static com.sequenceiq.cloudbreak.cluster.status.KraftMigrationStatus.BROKERS_IN_MIGRATION;
import static com.sequenceiq.cloudbreak.cluster.status.KraftMigrationStatus.NOT_APPLICABLE;
import static com.sequenceiq.cloudbreak.cluster.status.KraftMigrationStatus.PRE_MIGRATION;
import static com.sequenceiq.cloudbreak.cluster.status.KraftMigrationStatus.ZOOKEEPER_INSTALLED;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_2_17;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_3_2;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_3_2_10000;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cluster.service.ClouderaManagerProductsProvider;
import com.sequenceiq.cloudbreak.cluster.status.KraftMigrationStatus;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.util.CdhVersionProvider;

@Component
public class ZookeeperToKraftMigrationValidator {

    private static final Set<KraftMigrationStatus> INVALID_KRAFT_MIGRATION_STATUSES = Set.of(BROKERS_IN_MIGRATION, NOT_APPLICABLE);

    private static final Set<KraftMigrationStatus> INVALID_KRAFT_MIGRATION_FINALIZATION_STATUSES = Set.of(ZOOKEEPER_INSTALLED, PRE_MIGRATION,
            BROKERS_IN_MIGRATION, NOT_APPLICABLE);

    private final EntitlementService entitlementService;

    private final BlueprintService blueprintService;

    private final ClouderaManagerProductsProvider clouderaManagerProductsProvider;

    public ZookeeperToKraftMigrationValidator(EntitlementService entitlementService, BlueprintService blueprintService,
            ClouderaManagerProductsProvider clouderaManagerProductsProvider) {
        this.entitlementService = entitlementService;
        this.blueprintService = blueprintService;
        this.clouderaManagerProductsProvider = clouderaManagerProductsProvider;
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

        String bluePrintStackVersion = stack.getBlueprint().getStackVersion();
        if (!isZookeeperToKRaftMigrationSupportedForBluePrintVersion(bluePrintStackVersion)) {
            throw new BadRequestException("Zookeeper to KRaft migration is currently unavailable for clusters originally created with Runtime "
                    + bluePrintStackVersion + ". Please contact Cloudera Support for assistance.");
        }

        if (!entitlementService.isZookeeperToKRaftMigrationEnabled(accountId)) {
            throw new BadRequestException(String.format("Your account is not entitled to perform Zookeeper to KRaft migration. Please contact Cloudera " +
                    "to enable '%s' entitlement for your account.", CDP_ENABLE_ZOOKEEPER_TO_KRAFT_MIGRATION));
        }
    }

    public void validateZookeeperToKraftMigrationRuntimeVersion(StackDto stack) {
        String cdhVersion = getCdhVersionWithPatch(stack).orElseThrow(() -> new BadRequestException(
                "Cannot determine the Cloudera Runtime version including the patch version. Zookeeper to KRaft migration cannot be started."));
        if (!isZookeeperToKRaftMigrationSupportedForCdhVersion(cdhVersion)) {
            throw new BadRequestException("Zookeeper to KRaft migration is supported only for Cloudera Runtime version "
                    + CLOUDERA_STACK_VERSION_7_3_2_10000.getVersion() + " or higher. Current Cloudera Runtime version is: " + cdhVersion);
        }
    }

    public boolean isMigrationFromZookeeperToKraftSupported(StackDto stack, String accountId) {
        return isKraftMigrationStatusSupported(stack, accountId) && isZookeeperToKRaftMigrationSupportedForRuntimeVersion(stack);
    }

    public boolean isKraftMigrationStatusSupported(StackDto stack, String accountId) {
        boolean kraftMigrationEntitlementEnabled = entitlementService.isZookeeperToKRaftMigrationEnabled(accountId);
        return isKafkaServicePresent(stack) && isVersionNewerOrEqualThanLimited(stack.getStackVersion(), CLOUDERA_STACK_VERSION_7_3_2)
                && isZookeeperToKRaftMigrationSupportedForBluePrintVersion(stack.getBlueprint().getStackVersion()) && kraftMigrationEntitlementEnabled;
    }

    public boolean isZookeeperToKRaftMigrationSupportedForRuntimeVersion(StackDto stack) {
        return getCdhVersionWithPatch(stack)
                .map(this::isZookeeperToKRaftMigrationSupportedForCdhVersion)
                .orElse(false);
    }

    private boolean isZookeeperToKRaftMigrationSupportedForCdhVersion(String version) {
        return isVersionNewerOrEqualThanLimited(version, CLOUDERA_STACK_VERSION_7_3_2_10000);
    }

    private Optional<String> getCdhVersionWithPatch(StackDto stack) {
        return clouderaManagerProductsProvider.findCdhProduct(stack.getClusterComponents())
                .map(ClouderaManagerProduct::getVersion)
                .flatMap(version -> CdhVersionProvider.getCdhStackVersionFromVersionString(version)
                        .flatMap(runtimeVersion -> CdhVersionProvider.getCdhPatchVersionFromVersionString(version)
                                .map(patchVersion -> runtimeVersion + "." + patchVersion)));
    }

    private boolean isZookeeperToKRaftMigrationSupportedForBluePrintVersion(String version) {
        return isVersionNewerOrEqualThanLimited(version, CLOUDERA_STACK_VERSION_7_2_17);
    }

    private boolean isKafkaServicePresent(StackDto stack) {
        Predicate<Blueprint> bpPredicate =
                bp -> blueprintService.anyOfTheServiceTypesPresentOnBlueprint(bp.getBlueprintJsonText(), List.of("KAFKA"));
        return Optional.ofNullable(stack.getBlueprint())
                .filter(bpPredicate)
                .isPresent();
    }

}
