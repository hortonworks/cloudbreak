package com.sequenceiq.cloudbreak.service.rdsconfig;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class QueryProcessorRdsConfigProvider extends AbstractRdsConfigProvider {

    private static final String PILLAR_KEY = "query_processor";

    @Value("${cb.query_processor.database.user:query_processor}")
    private String dasDbUser;

    @Value("${cb.query_processor.database.db:query_processor}")
    private String dasDb;

    @Value("${cb.query_processor.database.port:5432}")
    private String dasDbPort;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Override
    protected String getDbUser() {
        return dasDbUser;
    }

    @Override
    protected String getDb() {
        return dasDb;
    }

    @Override
    protected String getDbPort() {
        return dasDbPort;
    }

    @Override
    protected String getPillarKey() {
        return PILLAR_KEY;
    }

    @Override
    protected DatabaseType getRdsType() {
        return DatabaseType.QUERY_PROCESSOR;
    }

    @Override
    protected boolean isRdsConfigNeeded(Blueprint blueprint, boolean hasGateway) {
        String blueprintText = blueprint.getBlueprintText();
        CmTemplateProcessor blueprintProcessor = cmTemplateProcessorFactory.get(blueprintText);
        return blueprintProcessor.isCMComponentExistsInBlueprint("QUERY_PROCESSOR");
    }
}
