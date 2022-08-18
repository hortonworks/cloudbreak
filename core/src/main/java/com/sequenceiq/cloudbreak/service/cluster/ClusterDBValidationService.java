package com.sequenceiq.cloudbreak.service.cluster;

import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigWithoutClusterService;
import com.sequenceiq.cloudbreak.view.ClusterView;

@Component
public class ClusterDBValidationService {
    private static final long REQUIRED_CM_DATABASE_COUNT = 2L;

    @Inject
    private RdsConfigWithoutClusterService rdsConfigWithoutClusterService;

    public boolean isGatewayRepairEnabled(ClusterView cluster) {
        return cluster.getEmbeddedDatabaseOnAttachedDisk() || isGatewayDatabaseAvailable(cluster);
    }

    private Boolean isGatewayDatabaseAvailable(ClusterView cluster) {
        if (cluster.getDatabaseServerCrn() != null) {
            return true;
        }
        long cmRdsCount = rdsConfigWithoutClusterService.countByClusterIdAndStatusInAndTypeIn(cluster.getId(), Set.of(ResourceStatus.USER_MANAGED),
                Set.of(DatabaseType.CLOUDERA_MANAGER, DatabaseType.CLOUDERA_MANAGER_MANAGEMENT_SERVICE_REPORTS_MANAGER));
        return cmRdsCount == REQUIRED_CM_DATABASE_COUNT;
    }
}
