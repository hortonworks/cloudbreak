package com.sequenceiq.cloudbreak.service.rdsconfig;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.domain.Blueprint;

@Component
public class SchemaRegistryServerRdsConfigProvider extends AbstractRdsConfigProvider {

    private static final String PILLAR_KEY = "schema_registry";

    @Value("${cb.schema_registry_server.database.port:5432}")
    private String port;

    @Value("${cb.schema_registry_server.database.user:schema_registry}")
    private String userName;

    @Value("${cb.schema_registry_server.database.db:schema_registry}")
    private String db;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

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
        return DatabaseType.REGISTRY;
    }

    @Override
    protected boolean isRdsConfigNeeded(Blueprint blueprint, boolean hasGateway) {
        CmTemplateProcessor blueprintProcessor = cmTemplateProcessorFactory.get(blueprint.getBlueprintText());
        return blueprintProcessor.isCMComponentExistsInBlueprint("SCHEMA_REGISTRY_SERVER");
    }
}
