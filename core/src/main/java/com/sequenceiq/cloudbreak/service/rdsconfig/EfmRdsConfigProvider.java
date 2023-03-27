package com.sequenceiq.cloudbreak.service.rdsconfig;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.domain.Blueprint;

@Component
public class EfmRdsConfigProvider extends AbstractRdsConfigProvider {
    private static final String PILLAR_KEY = "efm";

    @Value("${cb.efm.database.user:efm}")
    private String efmDbUser;

    @Value("${cb.efm.database.db:efm}")
    private String efmDb;

    @Value("${cb.efm.database.port:5432}")
    private String efmDbPort;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    private boolean isRdsConfigNeededForEfm(Blueprint blueprint) {
        String blueprintText = blueprint.getBlueprintText();
        CmTemplateProcessor cmTemplateProcessor = cmTemplateProcessorFactory.get(blueprintText);
        return cmTemplateProcessor.isCMComponentExistsInBlueprint("EFM_SERVER");
    }

    @Override
    protected String getDbUser() {
        return efmDbUser;
    }

    @Override
    protected String getDb() {
        return efmDb;
    }

    @Override
    protected String getDbPort() {
        return efmDbPort;
    }

    @Override
    protected String getPillarKey() {
        return PILLAR_KEY;
    }

    @Override
    protected DatabaseType getRdsType() {
        return DatabaseType.EFM;
    }

    @Override
    protected boolean isRdsConfigNeeded(Blueprint blueprint, boolean hasGateway) {
        return isRdsConfigNeededForEfm(blueprint);
    }
}
