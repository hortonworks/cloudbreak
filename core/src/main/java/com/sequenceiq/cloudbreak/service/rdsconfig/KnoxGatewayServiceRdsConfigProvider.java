package com.sequenceiq.cloudbreak.service.rdsconfig;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.domain.Blueprint;

@Component
public class KnoxGatewayServiceRdsConfigProvider extends AbstractRdsConfigProvider {

    private static final String PILLAR_KEY = "knox_gateway";

    @Value("${cb.knox_gateway.database.port:5432}")
    private String port;

    @Value("${cb.knox_gateway.database.user:knox_gateway}")
    private String userName;

    @Value("${cb.knox_gateway.database.db:knox_gateway}")
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
        return DatabaseType.KNOX_GATEWAY;
    }

    @Override
    protected boolean isRdsConfigNeeded(Blueprint blueprint, boolean hasGateway) {
        return hasGateway;
    }
}
