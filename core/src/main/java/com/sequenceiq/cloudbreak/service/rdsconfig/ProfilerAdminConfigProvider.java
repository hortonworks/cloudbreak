package com.sequenceiq.cloudbreak.service.rdsconfig;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class ProfilerAdminConfigProvider extends AbstractRdsConfigProvider {
    private static final String PILLAR_KEY = "profiler_admin";

    @Value("${cb.profiler.admin.database.user:profiler_agent}")
    private String profilerAdminDbUser;

    @Value("${cb.profiler.admin.database.db:profiler_agent}")
    private String profilerAdminDb;

    @Value("${cb.profiler.admin.database.port:5432}")
    private String profilerAdminDbPort;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    private boolean isRdsConfigNeedForProfilerAdmin(Blueprint blueprint) {
        String blueprintText = blueprint.getBlueprintText();
        CmTemplateProcessor cmTemplateProcessor = cmTemplateProcessorFactory.get(blueprintText);
        return cmTemplateProcessor.isComponentExistsInHostGroup("PROFILER_ADMIN_AGENT", "master");
    }

    @Override
    protected String getDbUser() {
        return profilerAdminDbUser;
    }

    @Override
    protected String getDb() {
        return profilerAdminDb;
    }

    @Override
    protected String getDbPort() {
        return profilerAdminDbPort;
    }

    @Override
    protected String getPillarKey() {
        return PILLAR_KEY;
    }

    @Override
    protected DatabaseType getRdsType() {
        return DatabaseType.PROFILER_AGENT;
    }

    @Override
    protected boolean isRdsConfigNeeded(Blueprint blueprint) {
        return isRdsConfigNeedForProfilerAdmin(blueprint);
    }
}
