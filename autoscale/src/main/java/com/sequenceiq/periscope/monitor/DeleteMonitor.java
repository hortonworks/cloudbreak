package com.sequenceiq.periscope.monitor;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.PeriscopeJob;
import com.sequenceiq.periscope.domain.PeriscopeNode;
import com.sequenceiq.periscope.monitor.handler.ClusterDeleteHandler;
import com.sequenceiq.periscope.repository.PeriscopeJobRepository;
import com.sequenceiq.periscope.repository.PeriscopeNodeRepository;
import com.sequenceiq.periscope.service.ha.PeriscopeNodeConfig;

@Component
@ConditionalOnProperty(prefix = "periscope.enabledAutoscaleMonitors.delete-monitor", name = "enabled", havingValue = "true")
public class DeleteMonitor extends ClusterMonitor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteMonitor.class);

    private static final long DEFAULT_DELETE_DURATION_MS = TimeUnit.HOURS.toMillis(2);

    private ClusterDeleteHandler clusterDeleteHandler;

    private PeriscopeJobRepository periscopeJobRepository;

    private PeriscopeNodeRepository periscopeNodeRepository;

    private PeriscopeNodeConfig periscopeNodeConfig;

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
        evalContext(context);
        boolean leader = false;
        periscopeNodeConfig = getApplicationContext().getBean(PeriscopeNodeConfig.class);
        if (periscopeNodeConfig.getId() != null) {
            periscopeNodeRepository = getApplicationContext().getBean(PeriscopeNodeRepository.class);
            Optional<PeriscopeNode> periscopeNodeOptional = periscopeNodeRepository.findById(periscopeNodeConfig.getId());
            if (periscopeNodeOptional.isPresent()) {
                leader = periscopeNodeOptional.get().isLeader();
            }
            if (leader) {
                LOGGER.info("Execution for {} started", getIdentifier());
                long lastExecuted = System.currentTimeMillis();
                clusterDeleteHandler = getApplicationContext().getBean(ClusterDeleteHandler.class);
                periscopeJobRepository = getApplicationContext().getBean(PeriscopeJobRepository.class);
                PeriscopeJob periscopeJob = periscopeJobRepository.findById(getIdentifier()).orElse(new PeriscopeJob(getIdentifier(),
                        System.currentTimeMillis() - DEFAULT_DELETE_DURATION_MS));
                clusterDeleteHandler.deleteClusters(periscopeJob.getLastExecuted());
                periscopeJob.setLastExecuted(lastExecuted);
                periscopeJobRepository.save(periscopeJob);
                LOGGER.info("Execution for {} completed successfully with lastExecuted updated as {}", getIdentifier(), lastExecuted);
            } else {
                LOGGER.info("Node is not leader so execution of delete monitor will be skipped");
            }
        } else {
            LOGGER.warn("Node Id is not specified so execution of delete monitor will be skipped");
        }
    }
}
