package com.sequenceiq.cloudbreak.service.rdsconfig;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class ProfilerMetricsConfigProvider extends AbstractRdsConfigProvider {
    private static final String PILLAR_KEY = "profiler_metrics";

    @Value("${cb.profiler.metrics.database.user:profiler_metric}")
    private String profilerMetricsDbUser;

    @Value("${cb.profiler.metrics.database.db:profiler_metric}")
    private String profilerMetricsDb;

    @Value("${cb.profiler.metrics.database.port:5432}")
    private String profilerMetricsDbPort;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    private boolean isRdsConfigNeedForProfilerMetrics(Blueprint blueprint) {
        String blueprintText = blueprint.getBlueprintText();
        CmTemplateProcessor cmTemplateProcessor = cmTemplateProcessorFactory.get(blueprintText);
        return cmTemplateProcessor.isCMComponentExistsInBlueprint("PROFILER_METRICS_AGENT");
    }

    @Override
    protected String getDbUser() {
        return profilerMetricsDbUser;
    }

    @Override
    protected String getDb() {
        return profilerMetricsDb;
    }

    @Override
    protected String getDbPort() {
        return profilerMetricsDbPort;
    }

    @Override
    protected String getPillarKey() {
        return PILLAR_KEY;
    }

    @Override
    protected DatabaseType getRdsType() {
        return DatabaseType.PROFILER_METRIC;
    }

    @Override
    protected boolean isRdsConfigNeeded(Blueprint blueprint, boolean hasGateway) {
        return isRdsConfigNeedForProfilerMetrics(blueprint);
    }
}
