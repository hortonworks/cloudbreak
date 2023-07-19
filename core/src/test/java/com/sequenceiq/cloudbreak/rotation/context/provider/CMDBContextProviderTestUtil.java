package com.sequenceiq.cloudbreak.rotation.context.provider;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;

public class CMDBContextProviderTestUtil {

    private CMDBContextProviderTestUtil() {

    }

    public static StackDto mockStack(Cluster cluster, String crn) {
        StackDto stackDto = mock(StackDto.class);
        when(stackDto.getCluster()).thenReturn(cluster);
        when(stackDto.getResourceCrn()).thenReturn(crn);
        return stackDto;
    }

    public static GatewayConfig mockGwConfig() {
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        lenient().when(gatewayConfig.getHostname()).thenReturn("host");
        return gatewayConfig;
    }

    public static Cluster mockCluster(Long clusterId) {
        Cluster cluster = mock(Cluster.class);
        lenient().when(cluster.getId()).thenReturn(clusterId);
        return cluster;
    }

    public static RDSConfig mockRdsConfig(DatabaseType databaseType) {
        RDSConfig rdsConfig = mock(RDSConfig.class);
        when(rdsConfig.getConnectionUserNameSecret()).thenReturn("userpath");
        when(rdsConfig.getConnectionPasswordSecret()).thenReturn("passpath");
        when(rdsConfig.getType()).thenReturn(databaseType.name());
        return rdsConfig;
    }
}
