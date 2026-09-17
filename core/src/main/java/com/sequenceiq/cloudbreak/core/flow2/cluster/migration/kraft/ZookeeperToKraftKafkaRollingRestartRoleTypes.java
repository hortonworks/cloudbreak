package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.sequenceiq.cloudbreak.cluster.api.ClusterModificationService;

public final class ZookeeperToKraftKafkaRollingRestartRoleTypes {

    private static final String KAFKA_SERVICE_TYPE = "KAFKA";

    private static final String KAFKA_KRAFT_ROLE = "KRAFT";

    private static final String KAFKA_BROKER_ROLE = "KAFKA_BROKER";

    private static final String KAFKA_CONNECT_ROLE = "KAFKA_CONNECT";

    private static final Set<String> KAFKA_ROLE_TYPES = Set.of(KAFKA_KRAFT_ROLE, KAFKA_BROKER_ROLE, KAFKA_CONNECT_ROLE);

    private ZookeeperToKraftKafkaRollingRestartRoleTypes() {
    }

    /**
     * Determines which Kafka role types to rolling-restart before the Zookeeper to KRaft migration, restricted to roles that are currently
     * active (a rolling restart only acts on active roles). Broker and Kafka Connect roles are always included when active; the KRaft role is
     * only included when it may carry pending config changes to pick up, i.e. when restarting stale configs only or when a dedicated KRaft host
     * group is present.
     */
    public static List<String> resolve(ClusterModificationService clusterModificationService, String clusterName, boolean staleConfigsOnly,
            boolean kraftHostGroupPresent) {
        Set<String> activeRoleTypes = clusterModificationService.getActiveServiceRoleTypes(clusterName, KAFKA_SERVICE_TYPE, KAFKA_ROLE_TYPES);
        List<String> roleTypes = new ArrayList<>();
        if ((staleConfigsOnly || kraftHostGroupPresent) && activeRoleTypes.contains(KAFKA_KRAFT_ROLE)) {
            roleTypes.add(KAFKA_KRAFT_ROLE);
        }
        if (activeRoleTypes.contains(KAFKA_BROKER_ROLE)) {
            roleTypes.add(KAFKA_BROKER_ROLE);
        }
        if (activeRoleTypes.contains(KAFKA_CONNECT_ROLE)) {
            roleTypes.add(KAFKA_CONNECT_ROLE);
        }
        return roleTypes;
    }

    /**
     * After a rollback, KRaft roles are stopped while brokers keep running. A re-migrate uses {@code staleConfigsOnly=true}, but rolling
     * restart only acts on active roles, so stopped KRaft never picks up pending config and CM rejects {@code KRaftMigrationCommand} with
     * stale state. CM does not reliably expose that staleness on stopped roles via the API (SUMMARY/FULL role reads and the UI can still
     * look fresh), so on the re-migrate path we deploy client configuration whenever KRaft roles are inactive.
     */
    public static boolean shouldDeployKafkaClientConfigBeforeRestart(ClusterModificationService clusterModificationService, String clusterName,
            boolean staleConfigsOnly) {
        return staleConfigsOnly && clusterModificationService.getInactiveServiceRoleTypes(clusterName, KAFKA_SERVICE_TYPE, Set.of(KAFKA_KRAFT_ROLE))
                .contains(KAFKA_KRAFT_ROLE);
    }
}
