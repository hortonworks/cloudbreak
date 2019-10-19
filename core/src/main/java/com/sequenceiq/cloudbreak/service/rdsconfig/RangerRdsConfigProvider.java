package com.sequenceiq.cloudbreak.service.rdsconfig;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;

@Component
public class RangerRdsConfigProvider extends AbstractRdsConfigProvider {

    private static final String PILLAR_KEY = "ranger";

    @Value("${cb.ranger.database.user:ranger}")
    private String rangerDbUser;

    @Value("${cb.ranger.database.db:ranger}")
    private String rangerDb;

    @Value("${cb.ranger.database.port:5432}")
    private String rangerDbPort;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Inject
    private BlueprintService blueprintService;

    private boolean isRdsConfigNeedForRangerAdmin(Blueprint blueprint) {
        String blueprintText = blueprint.getBlueprintText();
        CmTemplateProcessor blueprintProcessor = cmTemplateProcessorFactory.get(blueprintText);
        return blueprintProcessor.isCMComponentExistsInBlueprint("RANGER_ADMIN");
    }

    @Override
    protected String getDbUser() {
        return rangerDbUser;
    }

    @Override
    protected String getDb() {
        return rangerDb;
    }

    @Override
    protected String getDbPort() {
        return rangerDbPort;
    }

    @Override
    protected String getPillarKey() {
        return PILLAR_KEY;
    }

    @Override
    protected DatabaseType getRdsType() {
        return DatabaseType.RANGER;
    }

    @Override
    protected boolean isRdsConfigNeeded(Blueprint blueprint) {
        return isRdsConfigNeedForRangerAdmin(blueprint);
    }
}
