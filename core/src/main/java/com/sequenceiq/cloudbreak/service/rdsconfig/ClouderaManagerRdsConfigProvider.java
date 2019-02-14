package com.sequenceiq.cloudbreak.service.rdsconfig;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.domain.ClusterDefinition;

@Component
public class ClouderaManagerRdsConfigProvider extends AbstractRdsConfigProvider {

    private static final String PILLAR_KEY = "clouderamanager";

    @Value("${cb.clouderamanager.database.port:5432}")
    private String port;

    @Value("${cb.clouderamanager.database.user:clouderamanager}")
    private String userName;

    @Value("${cb.clouderamanager.database.db:clouderamanager}")
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
        return DatabaseType.CLOUDERA_MANAGER;
    }

    @Override
    protected boolean isRdsConfigNeeded(ClusterDefinition clusterDefinition) {
        return true;
    }

}
