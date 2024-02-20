package com.sequenceiq.cloudbreak.service.rdsconfig;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.domain.Blueprint;

@Component
public class ProfilerAdminRdsConfigProvider extends AbstractRdsConfigProvider {
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
        String blueprintText = blueprint.getBlueprintJsonText();
        CmTemplateProcessor cmTemplateProcessor = cmTemplateProcessorFactory.get(blueprintText);
        return cmTemplateProcessor.doesCMComponentExistsInBlueprint("PROFILER_ADMIN_AGENT");
    }

    @Override
    public String getDbUser() {
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
    public DatabaseType getRdsType() {
        return DatabaseType.PROFILER_AGENT;
    }

    @Override
    protected boolean isRdsConfigNeeded(Blueprint blueprint, boolean hasGateway, boolean cdl) {
        return isRdsConfigNeedForProfilerAdmin(blueprint);
    }
}
