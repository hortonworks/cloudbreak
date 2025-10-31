package com.sequenceiq.cloudbreak.service.rdsconfig;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.dataviz.DatavizRoles;
import com.sequenceiq.cloudbreak.domain.Blueprint;

@Component
public class DatavizRdsConfigProvider extends AbstractRdsConfigProvider {
    private static final String PILLAR_KEY = "dataviz";

    @Value("${cb.dataviz.database.user:dataviz}")
    private String datavizDbUser;

    @Value("${cb.dataviz.database.db:dataviz}")
    private String datavizDb;

    @Value("${cb.dataviz.database.port:5432}")
    private String datavizDbPort;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    private boolean isRdsConfigNeedForDataviz(Blueprint blueprint) {
        String blueprintText = blueprint.getBlueprintJsonText();
        CmTemplateProcessor cmTemplateProcessor = cmTemplateProcessorFactory.get(blueprintText);
        return cmTemplateProcessor.doesCMComponentExistsInBlueprint(DatavizRoles.DATAVIZ_WEBSERVER);
    }

    @Override
    public String getDbUser() {
        return datavizDbUser;
    }

    @Override
    public String getDb() {
        return datavizDb;
    }

    @Override
    protected String getDbPort() {
        return datavizDbPort;
    }

    @Override
    protected String getPillarKey() {
        return PILLAR_KEY;
    }

    @Override
    public DatabaseType getRdsType() {
        return DatabaseType.DATAVIZ;
    }

    @Override
    protected boolean isRdsConfigNeeded(Blueprint blueprint, boolean hasGateway) {
        return isRdsConfigNeedForDataviz(blueprint);
    }
}
