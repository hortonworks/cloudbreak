package com.sequenceiq.cloudbreak.service.rdsconfig;

import java.util.Locale;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.service.rdsconfig.cm.AbstractCdlRdsConfigProvider;

@Component
public class HiveCdlRdsConfigProvider extends AbstractCdlRdsConfigProvider {

    static final String HIVE_METASTORE_DATABASE_HOST = "hive_metastore_database_host";

    static final String HIVE_METASTORE_DATABASE_NAME = "hive_metastore_database_name";

    static final String HIVE_METASTORE_DATABASE_PASSWORD = "hive_metastore_database_password";

    static final String HIVE_METASTORE_DATABASE_PORT = "hive_metastore_database_port";

    static final String HIVE_METASTORE_DATABASE_USER = "hive_metastore_database_user";

    private static final String PILLAR_KEY = "cdl_hive";

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Override
    public DatabaseType getRdsType() {
        return DatabaseType.CDL_HIVE;
    }

    @Override
    public String getDbUser() {
        return HIVE_METASTORE_DATABASE_USER;
    }

    @Override
    protected String getDb() {
        return DatabaseType.HIVE.name().toLowerCase(Locale.ROOT);
    }

    @Override
    protected String getDbPort() {
        return HIVE_METASTORE_DATABASE_PORT;
    }

    @Override
    protected String getPillarKey() {
        return PILLAR_KEY;
    }

    @Override
    protected boolean isRdsConfigNeeded(Blueprint blueprint, boolean knoxGateway, boolean cdl) {
        String blueprintText = blueprint.getBlueprintJsonText();
        CmTemplateProcessor blueprintProcessor = cmTemplateProcessorFactory.get(blueprintText);
        return cdl && blueprintProcessor.isCMComponentExistsInBlueprint("HIVEMETASTORE");
    }

    @Override
    protected String getDbPassword() {
        return HIVE_METASTORE_DATABASE_PASSWORD;
    }

    @Override
    protected String getDbHost() {
        return HIVE_METASTORE_DATABASE_HOST;
    }

    @Override
    protected String getDbName() {
        return HIVE_METASTORE_DATABASE_NAME;
    }
}
