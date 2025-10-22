package com.sequenceiq.cloudbreak.service.validation;

import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_ENABLE_ZOOKEEPER_TO_KRAFT_MIGRATION;

import java.util.Comparator;
import java.util.Set;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
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

    public void validateZookeeperToKraftMigration(StackDto stack, String accountId) {
        if (!stack.getStatus().isAvailable()) {
            throw new BadRequestException("Zookeeper to KRaft migration can only be performed when the cluster is in Available state. " +
                    "Please ensure the cluster is fully operational before starting the migration.");
        }

        if (!hasStreamsMessagingTemplateType(stack)) {
            throw new BadRequestException("Zookeeper to KRaft migration is supported only for the following template types: "
                    + String.join(", ", kraftMigrationSupportedTemplates));
        }

        if (!isZookeeperToKRaftMigrationSupportedForStackVersion(stack.getStackVersion())) {
            throw new BadRequestException("Zookeeper to KRaft migration is supported only for CDP version " + ZOOKEEPER_TO_KRAFT_MIGRATION_MIN_VERSION
                    + " or higher");
        }

        if (!entitlementService.isZookeeperToKRaftMigrationEnabled(accountId)) {
            throw new BadRequestException(String.format("Your account is not entitled to perform Zookeeper to KRaft migration. Please contact Cloudera " +
                    "to enable '%s' entitlement for your account.", CDP_ENABLE_ZOOKEEPER_TO_KRAFT_MIGRATION));
        }
    }

    private boolean isZookeeperToKRaftMigrationSupportedForStackVersion(String version) {
        Comparator<Versioned> versionComparator = new VersionComparator();
        return versionComparator.compare(() -> version, () -> ZOOKEEPER_TO_KRAFT_MIGRATION_MIN_VERSION) >= 0;
    }

    private boolean hasStreamsMessagingTemplateType(StackDto stack) {
        if (stack.getBlueprint() == null || stack.getBlueprint().getName() == null) {
            return false;
        }

        String blueprintName = stack.getBlueprint().getName();

        return kraftMigrationSupportedTemplates.stream().anyMatch(blueprintName::contains);
    }
}