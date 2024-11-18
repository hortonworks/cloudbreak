package com.sequenceiq.cloudbreak.service.rdsconfig;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.domain.Blueprint;

@Component
public class DasRdsConfigProvider extends AbstractRdsConfigProvider {

    private static final String PILLAR_KEY = "das";

    @Value("${cb.das.database.user:das}")
    private String dasDbUser;

    @Value("${cb.das.database.db:das}")
    private String dasDb;

    @Value("${cb.das.database.port:5432}")
    private String dasDbPort;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Override
    public String getDbUser() {
        return dasDbUser;
    }

    @Override
    public String getDb() {
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
    public DatabaseType getRdsType() {
        return DatabaseType.HIVE_DAS;
    }

    @Override
    protected boolean isRdsConfigNeeded(Blueprint blueprint, boolean hasGateway) {
        String blueprintText = blueprint.getBlueprintJsonText();
        CmTemplateProcessor blueprintProcessor = cmTemplateProcessorFactory.get(blueprintText);
        return blueprintProcessor.doesCMComponentExistsInBlueprint("DAS_EVENT_PROCESSOR")
                || blueprintProcessor.doesCMComponentExistsInBlueprint("DAS_WEBAPP");

    }
}
