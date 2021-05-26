package com.sequenceiq.cloudbreak.service.rdsconfig;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.domain.Blueprint;

@Component
public class StreamsMessagingManagerRdsConfigProvider extends AbstractRdsConfigProvider {

    private static final String PILLAR_KEY = "smm";

    @Value("${cb.smm_server.database.port:5432}")
    private String port;

    @Value("${cb.smm_server.database.user:smm}")
    private String userName;

    @Value("${cb.smm_server.database.db:smm}")
    private String db;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

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
        return DatabaseType.STREAMS_MESSAGING_MANAGER;
    }

    @Override
    protected boolean isRdsConfigNeeded(Blueprint blueprint, boolean hasGateway) {
        CmTemplateProcessor blueprintProcessor = cmTemplateProcessorFactory.get(blueprint.getBlueprintText());
        return blueprintProcessor.isCMComponentExistsInBlueprint("STREAMS_MESSAGING_MANAGER_SERVER");
    }
}
