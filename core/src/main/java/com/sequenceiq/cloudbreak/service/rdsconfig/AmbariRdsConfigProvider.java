package com.sequenceiq.cloudbreak.service.rdsconfig;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.domain.ClusterDefinition;
import com.sequenceiq.cloudbreak.service.clusterdefinition.ClusterDefinitionService;

@Component
public class AmbariRdsConfigProvider extends AbstractRdsConfigProvider {

    private static final String PILLAR_KEY = "ambari";

    @Inject
    private ClusterDefinitionService clusterDefinitionService;

    @Value("${cb.ambari.database.port:5432}")
    private String port;

    @Value("${cb.ambari.database.user:ambari}")
    private String userName;

    @Value("${cb.ambari.database.db:ambari}")
    private String db;

    @Override
    protected String getDbUser() {
        return userName;
    }

    @Override
    protected String getDb() {
        return db;
    }

    @Override
    protected String getDbPort() {
        return port;
    }

    @Override
    protected String getPillarKey() {
        return PILLAR_KEY;
    }

    @Override
    protected DatabaseType getRdsType() {
        return DatabaseType.AMBARI;
    }

    @Override
    protected boolean isRdsConfigNeeded(ClusterDefinition clusterDefinition) {
        return clusterDefinitionService.isAmbariBlueprint(clusterDefinition);
    }
}
