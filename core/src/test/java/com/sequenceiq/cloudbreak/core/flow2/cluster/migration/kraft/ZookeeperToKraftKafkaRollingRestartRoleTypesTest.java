package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cluster.api.ClusterModificationService;

@ExtendWith(MockitoExtension.class)
class ZookeeperToKraftKafkaRollingRestartRoleTypesTest {

    private static final String CLUSTER_NAME = "testCluster";

    private static final String KAFKA_SERVICE_TYPE = "KAFKA";

    private static final String KAFKA_KRAFT_ROLE = "KRAFT";

    private static final String KAFKA_BROKER_ROLE = "KAFKA_BROKER";

    private static final String KAFKA_CONNECT_ROLE = "KAFKA_CONNECT";

    private static final List<String> KAFKA_ROLE_TYPES = List.of(KAFKA_KRAFT_ROLE, KAFKA_BROKER_ROLE, KAFKA_CONNECT_ROLE);

    @Mock
    private ClusterModificationService clusterModificationService;

    @Test
    void resolveShouldExcludeKraftOnInstallPath() {
        when(clusterModificationService.getActiveServiceRoleTypes(CLUSTER_NAME, KAFKA_SERVICE_TYPE, KAFKA_ROLE_TYPES))
                .thenReturn(List.of(KAFKA_BROKER_ROLE, KAFKA_CONNECT_ROLE));

        List<String> roleTypes = ZookeeperToKraftKafkaRollingRestartRoleTypes.resolve(clusterModificationService, CLUSTER_NAME, false, false);

        assertEquals(List.of(KAFKA_BROKER_ROLE, KAFKA_CONNECT_ROLE), roleTypes);
        verify(clusterModificationService).getActiveServiceRoleTypes(eq(CLUSTER_NAME), eq(KAFKA_SERVICE_TYPE), eq(KAFKA_ROLE_TYPES));
    }

    @Test
    void resolveShouldIncludeKraftOnUpscalePath() {
        when(clusterModificationService.getActiveServiceRoleTypes(CLUSTER_NAME, KAFKA_SERVICE_TYPE, KAFKA_ROLE_TYPES))
                .thenReturn(List.of(KAFKA_KRAFT_ROLE, KAFKA_BROKER_ROLE, KAFKA_CONNECT_ROLE));

        List<String> roleTypes = ZookeeperToKraftKafkaRollingRestartRoleTypes.resolve(clusterModificationService, CLUSTER_NAME, false, true);

        assertEquals(List.of(KAFKA_KRAFT_ROLE, KAFKA_BROKER_ROLE, KAFKA_CONNECT_ROLE), roleTypes);
    }

    @Test
    void resolveShouldIncludeActiveKraftOnReMigrate() {
        when(clusterModificationService.getActiveServiceRoleTypes(CLUSTER_NAME, KAFKA_SERVICE_TYPE, KAFKA_ROLE_TYPES))
                .thenReturn(List.of(KAFKA_KRAFT_ROLE, KAFKA_BROKER_ROLE));

        List<String> roleTypes = ZookeeperToKraftKafkaRollingRestartRoleTypes.resolve(clusterModificationService, CLUSTER_NAME, true, true);

        assertEquals(List.of(KAFKA_KRAFT_ROLE, KAFKA_BROKER_ROLE), roleTypes);
    }

    @Test
    void resolveShouldExcludeStoppedKraftOnReMigrate() {
        when(clusterModificationService.getActiveServiceRoleTypes(CLUSTER_NAME, KAFKA_SERVICE_TYPE, KAFKA_ROLE_TYPES))
                .thenReturn(List.of(KAFKA_BROKER_ROLE));

        List<String> roleTypes = ZookeeperToKraftKafkaRollingRestartRoleTypes.resolve(clusterModificationService, CLUSTER_NAME, true, true);

        assertEquals(List.of(KAFKA_BROKER_ROLE), roleTypes);
    }
}
