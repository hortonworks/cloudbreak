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

    private static final List<String> KAFKA_ROLE_TYPES = List.of(KAFKA_KRAFT_ROLE, KAFKA_BROKER_ROLE, KAFKA_CONNECT_ROLE);

    private ZookeeperToKraftKafkaRollingRestartRoleTypes() {
    }

    public static List<String> resolve(ClusterModificationService clusterModificationService, String clusterName, boolean staleConfigsOnly,
            boolean kraftHostGroupPresent) {
        Set<String> activeRoleTypes = Set.copyOf(clusterModificationService.getActiveServiceRoleTypes(clusterName, KAFKA_SERVICE_TYPE, KAFKA_ROLE_TYPES));
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
}
