package com.sequenceiq.cloudbreak.service.rdsconfig.cm;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.service.rdsconfig.AbstractRdsConfigProvider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ClouderaManagementServiceNavigatorMetaserverRdsConfigProvider extends AbstractRdsConfigProvider {

    private static final String PILLAR_KEY = "cmmanagement_navm";

    @Value("${cb.clouderamanager.management.service.port:5432}")
    private String port;

    @Value("${cb.clouderamanager.management.service.database.user:cmmanagement_navm}")
    private String userName;

    @Value("${cb.clouderamanager.management.service.database.db:cmmanagement_navm}")
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
        return DatabaseType.CLOUDERA_MANAGER_MANAGEMENT_SERVICE_NAVIGATOR_METASERVER;
    }

    @Override
    protected boolean isRdsConfigNeeded(Blueprint blueprint) {
        return true;
    }

}
