package com.sequenceiq.cloudbreak.service.upgrade.validation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradeImageInfo;

@ExtendWith(MockitoExtension.class)
class RuntimeUpgradeValidatorTest {

    private static final Versioned EXPECTED_MINIMAL_VERSION = CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_3_3;

    private static final Versioned BELOW_MINIMAL_VERSION = CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_3_2;

    private static final String VALIDATION_FAILED_MESSAGE = "In the selected runtime, Zookeeper is used as the broker for the Kafka service " +
            "instead of KRaft. This configuration is not supported and therefore blocks the runtime upgrade. " +
            "Please migrate your Kafka brokers to KRaft before proceeding with the upgrade.";

    private static final String CLUSTER_NAME = "test-cluster";

    private static final String KAFKA_BROKER_ROLE = "KAFKA_BROKER";

    private static final String KAFKA_SERVICE = "KAFKA";

    private static final String METADATA_STORE_CONFIG = "metadata.store";

    private static final String ZOOKEEPER_VALUE = "ZOOKEEPER";

    private static final String KRAFT_VALUE = "KRaft";

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

    private RuntimeUpgradeValidator underTest;

    @BeforeEach
    void setUp() {
        underTest = new RuntimeUpgradeValidator(clusterApiConnectors);
        lenient().when(mockStackDto.getStack()).thenReturn(mockStack);
        lenient().when(mockStackDto.getCluster()).thenReturn(mockCluster);
        lenient().when(mockCluster.getName()).thenReturn(CLUSTER_NAME);
    }

    @Test
    void testValidateShouldNotThrowExceptionWhenTargetRuntimeIsBelow733() {
        ServiceUpgradeValidationRequest request = createRequest(BELOW_MINIMAL_VERSION.getVersion());

        underTest.validate(request);

        verifyNoInteractions(clusterApiConnectors);
    }

    @Test
    void testValidateShouldNotThrowExceptionWhenTargetRuntimeIs733AndKafkaMetadataStoreIsNotPresent() {
        ServiceUpgradeValidationRequest request = createRequest(EXPECTED_MINIMAL_VERSION.getVersion());
        when(clusterApiConnectors.getConnector(mockStackDto)).thenReturn(mockClusterApi);
        when(mockClusterApi.getRoleConfigValueByServiceType(CLUSTER_NAME, KAFKA_BROKER_ROLE, KAFKA_SERVICE, METADATA_STORE_CONFIG))
                .thenReturn(Optional.empty());

        underTest.validate(request);

        verify(clusterApiConnectors).getConnector(mockStackDto);
        verify(mockClusterApi).getRoleConfigValueByServiceType(CLUSTER_NAME, KAFKA_BROKER_ROLE, KAFKA_SERVICE, METADATA_STORE_CONFIG);
    }

    @Test
    void testValidateShouldNotThrowExceptionWhenTargetRuntimeIs733AndKafkaMetadataStoreIsKRaft() {
        ServiceUpgradeValidationRequest request = createRequest(EXPECTED_MINIMAL_VERSION.getVersion());
        when(clusterApiConnectors.getConnector(mockStackDto)).thenReturn(mockClusterApi);
        when(mockClusterApi.getRoleConfigValueByServiceType(CLUSTER_NAME, KAFKA_BROKER_ROLE, KAFKA_SERVICE, METADATA_STORE_CONFIG))
                .thenReturn(Optional.of(KRAFT_VALUE));

        underTest.validate(request);

        verify(clusterApiConnectors).getConnector(mockStackDto);
        verify(mockClusterApi).getRoleConfigValueByServiceType(CLUSTER_NAME, KAFKA_BROKER_ROLE, KAFKA_SERVICE, METADATA_STORE_CONFIG);
    }

    @Test
    void testValidateShouldThrowExceptionWhenTargetRuntimeIs733AndKafkaMetadataStoreIsZookeeper() {
        ServiceUpgradeValidationRequest request = createRequest(EXPECTED_MINIMAL_VERSION.getVersion());
        when(clusterApiConnectors.getConnector(mockStackDto)).thenReturn(mockClusterApi);
        when(mockClusterApi.getRoleConfigValueByServiceType(CLUSTER_NAME, KAFKA_BROKER_ROLE, KAFKA_SERVICE, METADATA_STORE_CONFIG))
                .thenReturn(Optional.of(ZOOKEEPER_VALUE));

        UpgradeValidationFailedException exception = assertThrows(UpgradeValidationFailedException.class,
                () -> underTest.validate(request));

        assertEquals(VALIDATION_FAILED_MESSAGE, exception.getMessage());
        verify(clusterApiConnectors).getConnector(mockStackDto);
        verify(mockClusterApi).getRoleConfigValueByServiceType(CLUSTER_NAME, KAFKA_BROKER_ROLE, KAFKA_SERVICE, METADATA_STORE_CONFIG);
    }

    @Test
    void testValidateShouldThrowExceptionWhenTargetRuntimeIs733AndKafkaMetadataStoreIsZookeeperCaseInsensitive() {
        ServiceUpgradeValidationRequest request = createRequest(EXPECTED_MINIMAL_VERSION.getVersion());
        when(clusterApiConnectors.getConnector(mockStackDto)).thenReturn(mockClusterApi);
        when(mockClusterApi.getRoleConfigValueByServiceType(CLUSTER_NAME, KAFKA_BROKER_ROLE, KAFKA_SERVICE, METADATA_STORE_CONFIG))
                .thenReturn(Optional.of("zookeeper"));

        UpgradeValidationFailedException exception = assertThrows(UpgradeValidationFailedException.class,
                () -> underTest.validate(request));

        assertEquals(VALIDATION_FAILED_MESSAGE, exception.getMessage());
        verify(clusterApiConnectors).getConnector(mockStackDto);
        verify(mockClusterApi).getRoleConfigValueByServiceType(CLUSTER_NAME, KAFKA_BROKER_ROLE, KAFKA_SERVICE, METADATA_STORE_CONFIG);
    }

    @Test
    void testValidateShouldNotThrowExceptionWhenTargetRuntimeIsNewerOrEqualThan733AndKafkaMetadataStoreIsKRaft() {
        ServiceUpgradeValidationRequest request = createRequest(EXPECTED_MINIMAL_VERSION.getVersion());
        when(clusterApiConnectors.getConnector(mockStackDto)).thenReturn(mockClusterApi);
        when(mockClusterApi.getRoleConfigValueByServiceType(CLUSTER_NAME, KAFKA_BROKER_ROLE, KAFKA_SERVICE, METADATA_STORE_CONFIG))
                .thenReturn(Optional.of(KRAFT_VALUE));

        underTest.validate(request);

        verify(clusterApiConnectors).getConnector(mockStackDto);
        verify(mockClusterApi).getRoleConfigValueByServiceType(CLUSTER_NAME, KAFKA_BROKER_ROLE, KAFKA_SERVICE, METADATA_STORE_CONFIG);
    }

    @Test
    void testValidateShouldThrowExceptionWhenTargetRuntimeIsNewerOrEqualThan733AndKafkaMetadataStoreIsZookeeper() {
        ServiceUpgradeValidationRequest request = createRequest(EXPECTED_MINIMAL_VERSION.getVersion());
        when(clusterApiConnectors.getConnector(mockStackDto)).thenReturn(mockClusterApi);
        when(mockClusterApi.getRoleConfigValueByServiceType(CLUSTER_NAME, KAFKA_BROKER_ROLE, KAFKA_SERVICE, METADATA_STORE_CONFIG))
                .thenReturn(Optional.of(ZOOKEEPER_VALUE));

        UpgradeValidationFailedException exception = assertThrows(UpgradeValidationFailedException.class,
                () -> underTest.validate(request));

        assertEquals(VALIDATION_FAILED_MESSAGE, exception.getMessage());
        verify(clusterApiConnectors).getConnector(mockStackDto);
        verify(mockClusterApi).getRoleConfigValueByServiceType(CLUSTER_NAME, KAFKA_BROKER_ROLE, KAFKA_SERVICE, METADATA_STORE_CONFIG);
    }

    private ServiceUpgradeValidationRequest createRequest(String targetRuntimeVersion) {
        Image targetImage = mock(Image.class);
        when(targetImage.getVersion()).thenReturn(targetRuntimeVersion);

        StatedImage targetStatedImage = StatedImage.statedImage(targetImage, null, null);

        UpgradeImageInfo upgradeImageInfo = UpgradeImageInfo.builder()
                .withTargetStatedImage(targetStatedImage)
                .build();

        return new ServiceUpgradeValidationRequest(mockStackDto, false, true, upgradeImageInfo, false);
    }

}