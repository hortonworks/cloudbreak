package com.sequenceiq.cloudbreak.service.upgrade.validation.service;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_3_2;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_3_3;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionOlderThanLimited;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePropertiesTestUtils;
import com.sequenceiq.cloudbreak.service.upgrade.ServiceUpgradeValidationRequestTestUtils;

@ExtendWith(MockitoExtension.class)
class KafkaMetadataStoreUpgradeValidatorTest {

    private static final String CLUSTER_NAME = "test-cluster";

    private static final String KAFKA_BROKER_ROLE = "KAFKA_BROKER";

    private static final String KAFKA_SERVICE = "KAFKA";

    private static final String METADATA_STORE_CONFIG = "metadata.store";

    private static final String ZOOKEEPER_VALIDATION_MESSAGE = """
            In the selected runtime, ZooKeeper is used as the Kafka metadata store.
            This configuration is not supported for the target runtime.
            Please migrate your Kafka brokers to KRaft before proceeding with the upgrade.
            """.trim();

    private static final String KRAFT_BY_DEFAULT_VALIDATION_MESSAGE = """
            Cluster upgrade is blocked because the cluster was originally created with KRaft as the default Kafka metadata store.
            Upgrading from runtime 7.3.1 or lower to 7.3.2 or higher with this configuration is not supported.
            """.trim();

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private ClusterApi mockClusterApi;

    @Mock
    private StackDto mockStackDto;

    @Mock
    private Stack mockStack;

    @Mock
    private Cluster mockCluster;

    private KafkaMetadataStoreUpgradeValidator underTest;

    @BeforeEach
    void setUp() {
        underTest = new KafkaMetadataStoreUpgradeValidator(clusterApiConnectors);
        lenient().when(mockStackDto.getStack()).thenReturn(mockStack);
        lenient().when(mockStackDto.getCluster()).thenReturn(mockCluster);
        lenient().when(mockCluster.getName()).thenReturn(CLUSTER_NAME);
    }

    // --- Zookeeper validation ---

    @ParameterizedTest(name = "target={0}, metadataStore={1}, shouldThrow={2}")
    @CsvSource({
            "7.3.3, ZOOKEEPER, true",
            "7.3.3, zookeeper, true",
            "7.3.3, KRaft, false",
            "7.3.2, KRaft, false",
            "7.3.2, ZOOKEEPER, false",
            "7.3.1, ZOOKEEPER, false"
    })
    void testZookeeperValidation(String targetVersion, String metadataStore, boolean shouldThrow) {
        ServiceUpgradeValidationRequest request = createRequest(targetVersion);
        mockKafkaMetadataStore(metadataStore);

        if (shouldThrow) {
            UpgradeValidationFailedException ex = assertThrows(UpgradeValidationFailedException.class, () -> underTest.validate(request));
            assertEquals(ZOOKEEPER_VALIDATION_MESSAGE, ex.getMessage());
        } else {
            underTest.validate(request);
        }

        boolean shouldCallConnector = isVersionNewerOrEqualThanLimited(
                targetVersion, CLOUDERA_STACK_VERSION_7_3_3);

        if (shouldCallConnector) {
            verify(clusterApiConnectors).getConnector(mockStackDto);
            verify(mockClusterApi).getRoleConfigValueByServiceType(CLUSTER_NAME, KAFKA_BROKER_ROLE, KAFKA_SERVICE, METADATA_STORE_CONFIG);
        } else {
            verifyNoInteractions(clusterApiConnectors);
        }
    }

    // --- KRaft-by-default validation ---

    @ParameterizedTest(name = "current={0}, target={1}, metadataStore={2}, shouldThrow={3}")
    @CsvSource({
            "7.3.1, 7.3.2, KRaft, true",
            "7.3.1, 7.3.2, kraft, true",
            "7.2.18, 7.3.2, KRaft, true",
            "7.3.2, 7.3.3, KRaft, false",
            "7.3.1, 7.3.2, ZOOKEEPER, false",
            "7.3.1, 7.3.2, , false"
    })
    void testKraftByDefaultValidation(String currentVersion, String targetVersion, String metadataStore, boolean shouldThrow) {
        ServiceUpgradeValidationRequest request = createRequest(currentVersion, targetVersion);
        mockKafkaMetadataStore(metadataStore);

        if (shouldThrow) {
            UpgradeValidationFailedException ex = assertThrows(UpgradeValidationFailedException.class, () -> underTest.validate(request));
            assertEquals(KRAFT_BY_DEFAULT_VALIDATION_MESSAGE, ex.getMessage());
        } else {
            underTest.validate(request);
        }

        boolean targetAtLeast733 = isVersionNewerOrEqualThanLimited(
                targetVersion, CLOUDERA_STACK_VERSION_7_3_3);

        boolean kraftByDefaultRange = isVersionOlderThanLimited(currentVersion, CLOUDERA_STACK_VERSION_7_3_2)
                && isVersionNewerOrEqualThanLimited(targetVersion, CLOUDERA_STACK_VERSION_7_3_2);

        boolean shouldCallConnector = targetAtLeast733 || kraftByDefaultRange;

        if (shouldCallConnector) {
            verify(clusterApiConnectors).getConnector(mockStackDto);
            verify(mockClusterApi).getRoleConfigValueByServiceType(CLUSTER_NAME, KAFKA_BROKER_ROLE, KAFKA_SERVICE, METADATA_STORE_CONFIG);
        } else {
            verifyNoInteractions(clusterApiConnectors);
        }
    }

    // --- Helper methods ---

    private void mockKafkaMetadataStore(String value) {
        lenient().when(clusterApiConnectors.getConnector(mockStackDto)).thenReturn(mockClusterApi);
        lenient().when(mockClusterApi.getRoleConfigValueByServiceType(CLUSTER_NAME, KAFKA_BROKER_ROLE, KAFKA_SERVICE, METADATA_STORE_CONFIG))
                .thenReturn(Optional.ofNullable(value));
    }

    private ServiceUpgradeValidationRequest createRequest(String targetRuntimeVersion) {
        return ServiceUpgradeValidationRequestTestUtils.of(mockStackDto,
                ClusterUpgradePropertiesTestUtils.withTargetRuntimeOnly(targetRuntimeVersion));
    }

    private ServiceUpgradeValidationRequest createRequest(String currentRuntimeVersion, String targetRuntimeVersion) {
        return ServiceUpgradeValidationRequestTestUtils.of(mockStackDto,
                ClusterUpgradePropertiesTestUtils.withCurrentAndTargetRuntime(currentRuntimeVersion, targetRuntimeVersion, false, true, false));
    }
}