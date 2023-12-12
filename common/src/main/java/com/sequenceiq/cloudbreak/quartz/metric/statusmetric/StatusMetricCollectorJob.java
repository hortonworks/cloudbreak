package com.sequenceiq.cloudbreak.quartz.metric.statusmetric;

import java.util.Optional;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.logger.MdcContextInfoProvider;
import com.sequenceiq.cloudbreak.quartz.MdcQuartzJob;

@ConditionalOnBean(StatusMetricCollector.class)
@Component
@DisallowConcurrentExecution
public class StatusMetricCollectorJob extends MdcQuartzJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(StatusMetricCollectorJob.class);

    @Inject
    private StatusMetricCollector statusMetricCollector;

    @Override
    protected void executeTracedJob(JobExecutionContext context) throws JobExecutionException {
        try {
            if (statusMetricCollector != null) {
                statusMetricCollector.collectStatusMetrics();
            } else {
                LOGGER.warn("No status metric collector implementation found!");
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to collect stack status metrics");
        }
    }

    @Nullable
    @Override
    protected Optional<MdcContextInfoProvider> getMdcContextConfigProvider() {
        return Optional.empty();
    }
}
