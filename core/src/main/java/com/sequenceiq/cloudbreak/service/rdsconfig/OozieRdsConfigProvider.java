package com.sequenceiq.cloudbreak.service.rdsconfig;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class OozieRdsConfigProvider extends AbstractRdsConfigProvider {

    private static final String PILLAR_KEY = "oozie";

    @Value("${cb.oozie.database.user:oozie}")
    private String oozieDbUser;

    @Value("${cb.oozie.database.db:oozie}")
    private String oozieDb;

    @Value("${cb.oozie.database.port:5432}")
    private String oozieDbPort;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    private boolean isRdsConfigNeedForOozieServer(Blueprint blueprint) {
        String blueprintText = blueprint.getBlueprintText();
        CmTemplateProcessor blueprintProcessor = cmTemplateProcessorFactory.get(blueprintText);
        return blueprintProcessor.isCMComponentExistsInBlueprint("OOZIE_SERVER");
    }

    @Override
    protected String getDbUser() {
        return oozieDbUser;
    }

    @Override
    protected String getDb() {
        return oozieDb;
    }

    @Override
    protected String getDbPort() {
        return oozieDbPort;
    }

    @Override
    protected String getPillarKey() {
        return PILLAR_KEY;
    }

    @Override
    protected DatabaseType getRdsType() {
        return DatabaseType.OOZIE;
    }

    @Override
    protected boolean isRdsConfigNeeded(Blueprint blueprint, boolean hasGateway) {
        return isRdsConfigNeedForOozieServer(blueprint);
    }
}