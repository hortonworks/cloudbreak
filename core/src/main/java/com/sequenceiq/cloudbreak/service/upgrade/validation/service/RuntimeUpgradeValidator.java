package com.sequenceiq.cloudbreak.service.upgrade.validation.service;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;

@Component
public class RuntimeUpgradeValidator implements ServiceUpgradeValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeUpgradeValidator.class);

    private static final String ZOOKEEPER_SERVICE_TYPE = "ZOOKEEPER";

    private static final Versioned EXPECTED_MINIMAL_VERSION = CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_3_3;

    private final ClusterApiConnectors clusterApiConnectors;

    public RuntimeUpgradeValidator(ClusterApiConnectors clusterApiConnectors) {
        this.clusterApiConnectors = clusterApiConnectors;
    }

    @Override
    public void validate(ServiceUpgradeValidationRequest validationRequest) {
        if (isTargetRuntimeSameOrNewerThanRequired(validationRequest)) {
            ClusterApi connector = clusterApiConnectors.getConnector(validationRequest.stack());
            Optional<String> kafkaMetadataStore = connector.getRoleConfigValueByServiceType(validationRequest.stack().getCluster().getName(),
                    "KAFKA_BROKER", "KAFKA", "metadata.store");
            if (isKafkaMetadataStorePresentAndBrokerIsZookeeper(kafkaMetadataStore)) {
                String msg = "In the selected runtime, Zookeeper is used as the broker for the Kafka service instead of KRaft. " +
                        "This configuration is not supported and therefore blocks the runtime upgrade. " +
                        "Please migrate your Kafka brokers to KRaft before proceeding with the upgrade.";
                LOGGER.warn(msg);
                throw new UpgradeValidationFailedException(msg);
            }
        }
    }

    private boolean isTargetRuntimeSameOrNewerThanRequired(ServiceUpgradeValidationRequest validationRequest) {
        return isVersionNewerOrEqualThanLimited(validationRequest.upgradeImageInfo().getTargetStatedImage().getImage().getVersion(), EXPECTED_MINIMAL_VERSION);
    }

    private boolean isKafkaMetadataStorePresentAndBrokerIsZookeeper(Optional<String> kafkaMetadataStore) {
        return kafkaMetadataStore.isPresent() && kafkaMetadataStore.get().equalsIgnoreCase(ZOOKEEPER_SERVICE_TYPE);
    }

}
