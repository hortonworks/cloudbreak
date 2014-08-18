package com.sequenceiq.periscope.monitor.event.handler;

import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.ClusterMetricsInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.model.Alarm;
import com.sequenceiq.periscope.model.ScalingPolicy;
import com.sequenceiq.periscope.monitor.event.ClusterMetricsUpdateEvent;
import com.sequenceiq.periscope.registry.Cluster;
import com.sequenceiq.periscope.service.ClusterNotFoundException;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.ScalingService;
import com.sequenceiq.periscope.utils.ClusterUtils;

@Component
public class ClusterMetricsWatcher implements ApplicationListener<ClusterMetricsUpdateEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterMetricsWatcher.class);
    private static final int SEC_IN_MS = 1000;

    @Autowired
    private ClusterService clusterService;
    @Autowired
    private ScalingService scalingService;

    @Override
    public void onApplicationEvent(ClusterMetricsUpdateEvent event) {
        try {
            Cluster cluster = clusterService.get(event.getClusterId());
            ClusterMetricsInfo metrics = event.getClusterMetricsInfo();
            cluster.updateMetrics(metrics);
            for (Alarm alarm : cluster.getAlarms()) {
                double value = getMetricValue(metrics, alarm);
                if (alarmHit(value, alarm)) {
                    ScalingPolicy scalingPolicy = alarm.getScalingPolicy();
                    scalingService.scale(cluster, scalingPolicy);
                }
            }
        } catch (ClusterNotFoundException e) {
            LOGGER.error("Cluster not found, id: " + event.getClusterId(), e);
        }
    }

    private double getMetricValue(ClusterMetricsInfo metrics, Alarm alarm) {
        switch (alarm.getMetric()) {
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

    private boolean alarmHit(double value, Alarm alarm) {
        switch (alarm.getComparisonOperator()) {
            case EQUALS:
                return isEqualsHits(value, alarm);
            case GREATER_OR_EQUAL_THAN:
                return isGreaterOrEqualHits(value, alarm);
            case GREATER_THAN:
                return isGreaterHits(value, alarm);
            case LESS_OR_EQUAL_THAN:
                return isLessOrEqualHits(value, alarm);
            case LESS_THAN:
                return isLessHits(value, alarm);
            default:
                return false;
        }
    }

    private boolean isEqualsHits(double value, Alarm alarm) {
        boolean result = false;
        if (value == alarm.getThreshold()) {
            result = setAndCheckTime(alarm);
        } else {
            resetTime(alarm);
        }
        return result;
    }

    private boolean isGreaterOrEqualHits(double value, Alarm alarm) {
        boolean result = false;
        if (value >= alarm.getThreshold()) {
            result = setAndCheckTime(alarm);
        } else {
            resetTime(alarm);
        }
        return result;
    }

    private boolean isGreaterHits(double value, Alarm alarm) {
        boolean result = false;
        if (value > alarm.getThreshold()) {
            result = setAndCheckTime(alarm);
        }
        return result;
    }

    private boolean isLessOrEqualHits(double value, Alarm alarm) {
        boolean result = false;
        if (value <= alarm.getThreshold()) {
            result = setAndCheckTime(alarm);
        }
        return result;
    }

    private boolean isLessHits(double value, Alarm alarm) {
        boolean result = false;
        if (value < alarm.getThreshold()) {
            result = setAndCheckTime(alarm);
        }
        return result;
    }

    private boolean setAndCheckTime(Alarm alarm) {
        boolean result = false;
        long hitsSince = alarm.getAlarmHitsSince();
        if (hitsSince == 0) {
            setCurrentTime(alarm);
        } else {
            result = System.currentTimeMillis() - hitsSince > alarm.getPeriod() * SEC_IN_MS;
        }
        return result;
    }

    private void resetTime(Alarm alarm) {
        setTime(alarm, 0);
    }

    private void setCurrentTime(Alarm alarm) {
        setTime(alarm, System.currentTimeMillis());
    }

    private void setTime(Alarm alarm, long time) {
        alarm.setAlarmHitsSince(time);
    }

}