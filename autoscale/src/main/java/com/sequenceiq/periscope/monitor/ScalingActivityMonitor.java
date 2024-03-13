package com.sequenceiq.periscope.monitor;

import org.quartz.JobExecutionContext;

import com.sequenceiq.cloudbreak.ha.NodeConfig;
import com.sequenceiq.periscope.config.ScalingActivityCleanupConfig;
import com.sequenceiq.periscope.model.ScalingActivities;
import com.sequenceiq.periscope.monitor.context.EvaluatorContext;
import com.sequenceiq.periscope.monitor.context.ScalingActivitiesEvaluatorContext;
import com.sequenceiq.periscope.monitor.evaluator.EvaluatorExecutor;
import com.sequenceiq.periscope.service.ScalingActivityService;
import com.sequenceiq.periscope.service.ha.PeriscopeNodeService;

public abstract class ScalingActivityMonitor extends AbstractMonitor<ScalingActivities> {

    private ScalingActivityService scalingActivityService;

    private NodeConfig periscopeNodeConfig;

    private PeriscopeNodeService periscopeNodeService;

    private ScalingActivityCleanupConfig cleanupConfig;

    public ScalingActivityService getScalingActivityService() {
        return scalingActivityService;
    }

    public ScalingActivityCleanupConfig getCleanupConfig() {
        return cleanupConfig;
    }

    public NodeConfig getPeriscopeNodeConfig() {
        return periscopeNodeConfig;
    }

    public PeriscopeNodeService getPeriscopeNodeService() {
        return periscopeNodeService;
    }

    @Override
    void evalContext(JobExecutionContext context) {
        super.evalContext(context);
        this.scalingActivityService = getApplicationContext().getBean(ScalingActivityService.class);
        this.cleanupConfig = getApplicationContext().getBean(ScalingActivityCleanupConfig.class);
        this.periscopeNodeConfig = getApplicationContext().getBean(NodeConfig.class);
        this.periscopeNodeService = getApplicationContext().getBean(PeriscopeNodeService.class);
    }

    @Override
    protected void save(ScalingActivities monitored) {

    }

    @Override
    protected EvaluatorExecutor getEvaluatorExecutorBean(ScalingActivities monitored) {
        return getApplicationContext().getBean(getEvaluatorType(monitored).getSimpleName(), EvaluatorExecutor.class);
    }

    @Override
    public EvaluatorContext getContext(ScalingActivities monitored) {
        return new ScalingActivitiesEvaluatorContext(monitored);
    }
}
