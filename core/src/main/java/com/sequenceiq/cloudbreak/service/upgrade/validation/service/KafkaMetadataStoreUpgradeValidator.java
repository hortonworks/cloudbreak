package com.sequenceiq.cloudbreak.service.upgrade.validation.service;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionOlderThanLimited;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradeImageInfo;

/**
 * Validates Kafka metadata store constraints during runtime upgrades.
 *
 * <p>
 * The following upgrade scenarios are blocked:
 * <ul>
 *   <li>Target runtime >= 7.3.3 while Kafka still uses ZooKeeper</li>
 *   <li>Upgrading from runtime <= 7.3.1 to >= 7.3.2 when the cluster was created with KRaft by default</li>
 * </ul>
 */
@Component
public class KafkaMetadataStoreUpgradeValidator implements ServiceUpgradeValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaMetadataStoreUpgradeValidator.class);

    private static final String ROLE_TYPE_KAFKA_BROKER = "KAFKA_BROKER";

    private static final String SERVICE_TYPE_KAFKA = "KAFKA";

    private static final String METADATA_STORE_CONFIG_KEY = "metadata.store";

    private static final String METADATA_STORE_ZOOKEEPER = "ZOOKEEPER";

    private static final String METADATA_STORE_KRAFT = "KRaft";

    private static final Versioned ZOOKEEPER_CHECK_MIN_TARGET_VERSION =
            CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_3_3;

    private static final Versioned KRAFT_BY_DEFAULT_BOUNDARY_VERSION =
            CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_3_2;

    private final ClusterApiConnectors clusterApiConnectors;

    public KafkaMetadataStoreUpgradeValidator(ClusterApiConnectors clusterApiConnectors) {
        this.clusterApiConnectors = clusterApiConnectors;
    }

    @Override
    public void validate(ServiceUpgradeValidationRequest request) {
        boolean targetAtLeast733 = isTargetVersionAtLeast733(request);
        boolean upgradeInKraftByDefaultRange = isUpgradeInKraftByDefaultRange(request);

        if (!targetAtLeast733 && !upgradeInKraftByDefaultRange) {
            return;
        }

        Optional<String> metadataStore = fetchKafkaMetadataStore(request);

        if (targetAtLeast733 && isKafkaMetadataStore(metadataStore, METADATA_STORE_ZOOKEEPER)) {
            String msg = """
                    In the selected runtime, ZooKeeper is used as the Kafka metadata store.
                    This configuration is not supported for the target runtime.
                    Please migrate your Kafka brokers to KRaft before proceeding with the upgrade.
                    """.trim();
            LOGGER.warn(msg);
            throw new UpgradeValidationFailedException(msg);
        }
        if (upgradeInKraftByDefaultRange && isKafkaMetadataStore(metadataStore, METADATA_STORE_KRAFT)) {
            String msg = """
                    Cluster upgrade is blocked because the cluster was originally created with KRaft as the default Kafka metadata store.
                    Upgrading from runtime 7.3.1 or lower to 7.3.2 or higher with this configuration is not supported.
                    """.trim();
            LOGGER.warn(msg);
            throw new UpgradeValidationFailedException(msg);
        }
    }

    private Optional<String> fetchKafkaMetadataStore(ServiceUpgradeValidationRequest request) {
        ClusterApi connector = clusterApiConnectors.getConnector(request.stack());
        return connector.getRoleConfigValueByServiceType(
                request.stack().getCluster().getName(),
                ROLE_TYPE_KAFKA_BROKER,
                SERVICE_TYPE_KAFKA,
                METADATA_STORE_CONFIG_KEY
        );
    }

    private boolean isTargetVersionAtLeast733(ServiceUpgradeValidationRequest request) {
        String targetVersion = getTargetVersion(request);
        return isVersionNewerOrEqualThanLimited(
                targetVersion,
                ZOOKEEPER_CHECK_MIN_TARGET_VERSION
        );
    }

    private boolean isUpgradeInKraftByDefaultRange(ServiceUpgradeValidationRequest request) {
        Optional<String> currentVersion = getCurrentRuntimeVersion(request);
        if (currentVersion.isEmpty()) {
            LOGGER.debug("Current runtime version is not available, skipping KRaft-by-default validation.");
            return false;
        }
        String targetVersion = getTargetVersion(request);
        return isVersionOlderThanLimited(currentVersion.get(), KRAFT_BY_DEFAULT_BOUNDARY_VERSION)
                && isVersionNewerOrEqualThanLimited(targetVersion, KRAFT_BY_DEFAULT_BOUNDARY_VERSION);
    }

    private Optional<String> getCurrentRuntimeVersion(ServiceUpgradeValidationRequest request) {
        if (request.clusterUpgradeProperties() != null && request.clusterUpgradeProperties().getCurrentRuntimeVersion() != null) {
            return Optional.of(request.clusterUpgradeProperties().getCurrentRuntimeVersion());
        }
        // TODO CB-33421: Remove upgradeImageInfo fallback once callers always pass clusterUpgradeProperties.
        return Optional.ofNullable(request.upgradeImageInfo())
                .map(UpgradeImageInfo::getCurrentImage)
                .map(img -> img.getPackageVersion(ImagePackageVersion.STACK));
    }

    private String getTargetVersion(ServiceUpgradeValidationRequest request) {
        if (request.clusterUpgradeProperties() != null) {
            if (request.clusterUpgradeProperties().getTargetImageVersion() != null) {
                return request.clusterUpgradeProperties().getTargetImageVersion();
            }
            return request.clusterUpgradeProperties().getRuntimeVersion();
        }
        // TODO CB-33421: Remove upgradeImageInfo fallback once callers always pass clusterUpgradeProperties.
        if (request.upgradeImageInfo() != null && request.upgradeImageInfo().getTargetStatedImage() != null) {
            return request.upgradeImageInfo().getTargetStatedImage().getImage().getVersion();
        }
        return null;
    }

    private boolean isKafkaMetadataStore(Optional<String> metadataStore, String expectedValue) {
        return metadataStore
                .map(value -> value.equalsIgnoreCase(expectedValue))
                .orElse(false);
    }
}