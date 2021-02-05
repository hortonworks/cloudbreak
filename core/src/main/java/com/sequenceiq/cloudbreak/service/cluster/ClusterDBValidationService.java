package com.sequenceiq.cloudbreak.service.cluster;

import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;

@Component
public class ClusterDBValidationService {
    private static final long REQUIRED_CM_DATABASE_COUNT = 2L;

    @Inject
    private RdsConfigService rdsConfigService;

    public Boolean isGatewayRepairEnabled(Cluster cluster) {
        return cluster.getEmbeddedDatabaseOnAttachedDisk() || isGatewayDatabaseAvailable(cluster);
    }

    private Boolean isGatewayDatabaseAvailable(Cluster cluster) {
        Set<RDSConfig> rdsConfigs = rdsConfigService.findByClusterId(cluster.getId());
        long cmRdsCount = rdsConfigs.stream()
                .filter(rds -> rds.getStatus() == ResourceStatus.USER_MANAGED)
                .map(RDSConfig::getType)
                .filter(type -> DatabaseType.CLOUDERA_MANAGER.name().equals(type)
                        || DatabaseType.CLOUDERA_MANAGER_MANAGEMENT_SERVICE_REPORTS_MANAGER.name().equals(type))
                .distinct()
                .count();
        return cmRdsCount == REQUIRED_CM_DATABASE_COUNT || cluster.getDatabaseServerCrn() != null;
    }
}
