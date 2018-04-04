package com.sequenceiq.periscope.monitor.evaluator;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.domain.BaseAlert;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.TimeAlert;
import com.sequenceiq.periscope.log.MDCBuilder;
import com.sequenceiq.periscope.monitor.MonitorUpdateRate;
import com.sequenceiq.periscope.monitor.event.ScalingEvent;
import com.sequenceiq.periscope.repository.TimeAlertRepository;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.utils.DateUtils;

@Component("CronTimeEvaluator")
@Scope("prototype")
public class CronTimeEvaluator extends AbstractEventPublisher implements EvaluatorExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CronTimeEvaluator.class);

    @Inject
    private TimeAlertRepository alertRepository;

    @Inject
    private ClusterService clusterService;

    @Inject
    private DateUtils dateUtils;

    private long clusterId;

    @Override
    public void setContext(Map<String, Object> context) {
        clusterId = (long) context.get(EvaluatorContext.CLUSTER_ID.name());
    }

    private boolean isTrigger(TimeAlert alert) {
        return dateUtils.isTrigger(alert, MonitorUpdateRate.CLUSTER_UPDATE_RATE);
    }

    private boolean isPolicyAttached(BaseAlert alert) {
        return alert.getScalingPolicy() != null;
    }

    @Override
    public void run() {
        Cluster cluster = clusterService.find(clusterId);
        MDCBuilder.buildMdcContext(cluster);

        for (TimeAlert alert : alertRepository.findAllByCluster(clusterId)) {
            if (isTrigger(alert) && isPolicyAttached(alert)) {
                LOGGER.info("Time alert '{}' triggers the '{}' scaling policy", alert.getName(), alert.getScalingPolicy().getName());
                publishEvent(new ScalingEvent(alert));
                break;
            }
        }
    }
}
