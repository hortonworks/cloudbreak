package com.sequenceiq.cloudbreak.service.rdsconfig;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.domain.Blueprint;

@Component
public class QueryProcessorRdsConfigProvider extends AbstractRdsConfigProvider {

    private static final String PILLAR_KEY = "query_processor";

    @Value("${cb.query_processor.database.user:query_processor}")
    private String qpDbUser;

    @Value("${cb.query_processor.database.db:query_processor}")
    private String qpDb;

    @Value("${cb.query_processor.database.port:5432}")
    private String qpDbPort;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Override
    public String getDbUser() {
        return qpDbUser;
    }

    @Override
    protected String getDb() {
        return qpDb;
    }

    @Override
    protected String getDbPort() {
        return qpDbPort;
    }

    @Override
    protected String getPillarKey() {
        return PILLAR_KEY;
    }

    @Override
    public DatabaseType getRdsType() {
        return DatabaseType.QUERY_PROCESSOR;
    }

    @Override
    protected boolean isRdsConfigNeeded(Blueprint blueprint, boolean hasGateway) {
        String blueprintText = blueprint.getBlueprintJsonText();
        CmTemplateProcessor blueprintProcessor = cmTemplateProcessorFactory.get(blueprintText);
        return blueprintProcessor.isCMComponentExistsInBlueprint("QUERY_PROCESSOR");
    }
}
