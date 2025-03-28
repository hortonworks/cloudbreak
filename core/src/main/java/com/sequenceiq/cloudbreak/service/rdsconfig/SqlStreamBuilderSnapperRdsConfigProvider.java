package com.sequenceiq.cloudbreak.service.rdsconfig;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ssb.SqlStreamBuilderRoles;
import com.sequenceiq.cloudbreak.domain.Blueprint;

@Component
public class SqlStreamBuilderSnapperRdsConfigProvider extends AbstractRdsConfigProvider {

    private static final String PILLAR_KEY = "ssb_snapper";

    @Value("${cb.ssb.database.snapper.port:5432}")
    private String port;

    @Value("${cb.ssb.database.snapper.user:ssb_mve}")
    private String userName;

    @Value("${cb.ssb.database.snapper.db:ssb_mve}")
    private String db;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Override
    public String getDbUser() {
        return userName;
    }

    @Override
    public String getDb() {
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
        return DatabaseType.SQL_STREAM_BUILDER_SNAPPER;
    }

    @Override
    protected boolean isRdsConfigNeeded(Blueprint blueprint, boolean hasGateway) {
        CmTemplateProcessor blueprintProcessor = cmTemplateProcessorFactory.get(blueprint.getBlueprintJsonText());
        return blueprintProcessor.doesCMComponentExistsInBlueprint(SqlStreamBuilderRoles.MATERIALIZED_VIEW_ENGINE);
    }
}
