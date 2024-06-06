package com.sequenceiq.cloudbreak.service.rdsconfig.cm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.service.rdsconfig.AbstractRdsConfigProvider;

@Deprecated
@Component
public class ClouderaManagementServiceReportsManagerRdsConfigProvider extends AbstractRdsConfigProvider {

    private static final String PILLAR_KEY = "cmmanagement_rm";

    @Value("${cb.clouderamanager.management.service.port:5432}")
    private String port;

    @Value("${cb.clouderamanager.management.service.database.user:cmmanagement_rm}")
    private String userName;

    @Value("${cb.clouderamanager.management.service.database.db:cmmanagement_rm}")
    private String db;

    @Override
    public String getDbUser() {
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
    public DatabaseType getRdsType() {
        return DatabaseType.CLOUDERA_MANAGER_MANAGEMENT_SERVICE_REPORTS_MANAGER;
    }

    @Override
    protected boolean isRdsConfigNeeded(Blueprint blueprint, boolean hasGateway) {
        return false;
    }

}
