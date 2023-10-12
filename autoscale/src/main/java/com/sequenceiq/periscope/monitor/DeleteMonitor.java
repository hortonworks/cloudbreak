package com.sequenceiq.periscope.monitor;

import java.util.concurrent.TimeUnit;

import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.PeriscopeJob;
import com.sequenceiq.periscope.monitor.handler.ClusterDeleteHandler;
import com.sequenceiq.periscope.repository.PeriscopeJobRepository;

@Component
@ConditionalOnProperty(prefix = "periscope.enabledAutoscaleMonitors.delete-monitor", name = "enabled", havingValue = "true")
public class DeleteMonitor extends ClusterMonitor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteMonitor.class);

    private static final long DEFAULT_DELETE_DURATION_MS = TimeUnit.HOURS.toMillis(2);

    private ClusterDeleteHandler clusterDeleteHandler;

    private PeriscopeJobRepository periscopeJobRepository;

    @Override
    public String getIdentifier() {
        return "delete-monitor";
    }

    @Override
    public String getTriggerExpression() {
        return MonitorUpdateRate.EVERY_TEN_MIN_RATE_CRON;
    }

    @Override
    public Class<?> getEvaluatorType(Cluster monitored) {
        return null;
    }

    @Override
    public void execute(JobExecutionContext context) {
        LOGGER.info("Execution for {} started", getIdentifier());
        long lastExecuted = System.currentTimeMillis();
        evalContext(context);
        clusterDeleteHandler = getApplicationContext().getBean(ClusterDeleteHandler.class);
        periscopeJobRepository = getApplicationContext().getBean(PeriscopeJobRepository.class);
        PeriscopeJob periscopeJob = periscopeJobRepository.findById(getIdentifier()).orElse(new PeriscopeJob(getIdentifier(),
                System.currentTimeMillis() - DEFAULT_DELETE_DURATION_MS));
        clusterDeleteHandler.deleteClusters(periscopeJob.getLastExecuted());
        periscopeJob.setLastExecuted(lastExecuted);
        periscopeJobRepository.save(periscopeJob);
        LOGGER.info("Execution for {} completed successfully with lastExecuted updated as {}", getIdentifier(), lastExecuted);
    }
}
