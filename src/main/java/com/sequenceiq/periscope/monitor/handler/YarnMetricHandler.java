package com.sequenceiq.periscope.monitor.handler;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.ClusterMetricsInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ComparisonOperator;
import com.sequenceiq.periscope.domain.MetricAlarm;
import com.sequenceiq.periscope.log.Logger;
import com.sequenceiq.periscope.log.PeriscopeLoggerFactory;
import com.sequenceiq.periscope.monitor.event.EventType;
import com.sequenceiq.periscope.monitor.event.YarnMetricUpdateEvent;
import com.sequenceiq.periscope.service.ClusterNotFoundException;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.utils.ClusterUtils;

@Component
public class YarnMetricHandler implements MetricHandler {

    private static final Logger LOGGER = PeriscopeLoggerFactory.getLogger(YarnMetricHandler.class);
    private static final DecimalFormat TIME_FORMAT = new DecimalFormat("##.##");

    @Autowired
    private ClusterService clusterService;

    @Override
    public List<MetricResult> isAlarmHit(ApplicationEvent event) {
        YarnMetricUpdateEvent source = (YarnMetricUpdateEvent) event;
        long clusterId = source.getClusterId();
        try {
            Cluster cluster = clusterService.get(clusterId);
            ClusterMetricsInfo metrics = source.getClusterMetricsInfo();
            return checkMetricAlarms(cluster, metrics);
        } catch (ClusterNotFoundException e) {
            LOGGER.error(clusterId, "Cluster not found, ignoring event", e);
        }
        return Collections.emptyList();
    }

    @Override
    public EventType getEventType() {
        return EventType.YARN_METRIC;
    }

    private List<MetricResult> checkMetricAlarms(Cluster cluster, ClusterMetricsInfo metrics) {
        List<MetricResult> result = new ArrayList<>();
        for (MetricAlarm metricAlarm : cluster.getMetricAlarms()) {
            long clusterId = cluster.getId();
            LOGGER.info(clusterId, "Checking metric based alarm: '{}'", metricAlarm.getName());
            double value = getMetricValue(metrics, metricAlarm);
            if (alarmHit(value, metricAlarm, clusterId)) {
                result.add(new MetricResult(true, metricAlarm, cluster));
            } else {
                result.add(new MetricResult(false, metricAlarm, cluster));
            }
        }
        return result;
    }

    private double getMetricValue(ClusterMetricsInfo metrics, MetricAlarm metricAlarm) {
        switch (metricAlarm.getMetric()) {
            case PENDING_CONTAINERS:
                return metrics.getPendingContainers();
            case PENDING_APPLICATIONS:
                return metrics.getAppsPending();
            case LOST_NODES:
                return metrics.getLostNodes();
            case UNHEALTHY_NODES:
                return metrics.getUnhealthyNodes();
            case GLOBAL_RESOURCES:
                return ClusterUtils.computeFreeClusterResourceRate(metrics);
            default:
                return 0;
        }
    }

    private boolean alarmHit(double value, MetricAlarm metricAlarm, long clusterId) {
        switch (metricAlarm.getComparisonOperator()) {
            case EQUALS:
                return isEqualsHits(value, metricAlarm, clusterId);
            case GREATER_OR_EQUAL_THAN:
                return isGreaterOrEqualHits(value, metricAlarm, clusterId);
            case GREATER_THAN:
                return isGreaterHits(value, metricAlarm, clusterId);
            case LESS_OR_EQUAL_THAN:
                return isLessOrEqualHits(value, metricAlarm, clusterId);
            case LESS_THAN:
                return isLessHits(value, metricAlarm, clusterId);
            default:
                return false;
        }
    }

    private boolean isEqualsHits(double value, MetricAlarm metricAlarm, long clusterId) {
        return isComparisonHit(value == metricAlarm.getThreshold(), metricAlarm, ComparisonOperator.EQUALS, clusterId);
    }

    private boolean isGreaterOrEqualHits(double value, MetricAlarm metricAlarm, long clusterId) {
        return isComparisonHit(value >= metricAlarm.getThreshold(), metricAlarm, ComparisonOperator.GREATER_OR_EQUAL_THAN, clusterId);
    }

    private boolean isGreaterHits(double value, MetricAlarm metricAlarm, long clusterId) {
        return isComparisonHit(value > metricAlarm.getThreshold(), metricAlarm, ComparisonOperator.GREATER_THAN, clusterId);
    }

    private boolean isLessOrEqualHits(double value, MetricAlarm metricAlarm, long clusterId) {
        return isComparisonHit(value <= metricAlarm.getThreshold(), metricAlarm, ComparisonOperator.LESS_OR_EQUAL_THAN, clusterId);
    }

    private boolean isLessHits(double value, MetricAlarm metricAlarm, long clusterId) {
        return isComparisonHit(value < metricAlarm.getThreshold(), metricAlarm, ComparisonOperator.LESS_THAN, clusterId);
    }

    private boolean isComparisonHit(boolean valueHit, MetricAlarm metricAlarm, ComparisonOperator operator, long clusterId) {
        boolean result = false;
        String alarmName = metricAlarm.getName();
        if (valueHit) {
            LOGGER.info(clusterId, "{} comparison hit for alarm: '{}'", operator, alarmName);
            result = setAndCheckTime(metricAlarm, clusterId);
        } else {
            LOGGER.info(clusterId, "{} comparison failed for alarm: '{}'", operator, alarmName);
            reset(metricAlarm);
        }
        return result;
    }

    private boolean setAndCheckTime(MetricAlarm metricAlarm, long clusterId) {
        boolean result = false;
        String alarmName = metricAlarm.getName();
        long hitsSince = metricAlarm.getAlarmHitsSince();
        if (hitsSince == 0) {
            LOGGER.info(clusterId, "Counter starts until hit for alarm: '{}'", alarmName);
            setCurrentTime(metricAlarm);
        } else {
            long elapsedTime = System.currentTimeMillis() - hitsSince;
            result = elapsedTime > (metricAlarm.getPeriod() * ClusterUtils.MIN_IN_MS);
            LOGGER.info(clusterId, "Alarm: '{}' stands since {} minutes", alarmName,
                    TIME_FORMAT.format((double) elapsedTime / ClusterUtils.MIN_IN_MS));
            if (result) {
                LOGGER.info(clusterId, "Alarm: '{}' HIT", alarmName);
            }
        }
        return result;
    }

    private void reset(MetricAlarm metricAlarm) {
        metricAlarm.reset();
    }

    private void setCurrentTime(MetricAlarm metricAlarm) {
        metricAlarm.setAlarmHitsSince(System.currentTimeMillis());
    }
}
