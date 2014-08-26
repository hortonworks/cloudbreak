package com.sequenceiq.periscope.monitor.event.handler;

import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.ClusterMetricsInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.domain.Alarm;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ComparisonOperator;
import com.sequenceiq.periscope.domain.Notification;
import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.log.Logger;
import com.sequenceiq.periscope.log.PeriscopeLoggerFactory;
import com.sequenceiq.periscope.monitor.event.ClusterMetricsUpdateEvent;
import com.sequenceiq.periscope.service.ClusterNotFoundException;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.NotificationService;
import com.sequenceiq.periscope.service.ScalingService;
import com.sequenceiq.periscope.utils.ClusterUtils;

@Component
public class ClusterMetricsWatcher implements ApplicationListener<ClusterMetricsUpdateEvent> {

    private static final Logger LOGGER = PeriscopeLoggerFactory.getLogger(ClusterMetricsWatcher.class);

    @Autowired
    private ClusterService clusterService;
    @Autowired
    private ScalingService scalingService;
    @Autowired
    private NotificationService notificationService;

    @Override
    public void onApplicationEvent(ClusterMetricsUpdateEvent event) {
        long clusterId = event.getClusterId();
        try {
            Cluster cluster = clusterService.get(clusterId);
            ClusterMetricsInfo metrics = event.getClusterMetricsInfo();
            cluster.updateMetrics(metrics);
            for (Alarm alarm : cluster.getAlarms()) {
                LOGGER.info(clusterId, "Checking alarm: {}", alarm.getName());
                double value = getMetricValue(metrics, alarm);
                if (alarmHit(value, alarm, clusterId)) {
                    handleNotifications(cluster, alarm);
                    handleScaling(cluster, alarm);
                }
            }
        } catch (ClusterNotFoundException e) {
            LOGGER.error(clusterId, "Cluster not found", e);
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

    private boolean alarmHit(double value, Alarm alarm, long clusterId) {
        switch (alarm.getComparisonOperator()) {
            case EQUALS:
                return isEqualsHits(value, alarm, clusterId);
            case GREATER_OR_EQUAL_THAN:
                return isGreaterOrEqualHits(value, alarm, clusterId);
            case GREATER_THAN:
                return isGreaterHits(value, alarm, clusterId);
            case LESS_OR_EQUAL_THAN:
                return isLessOrEqualHits(value, alarm, clusterId);
            case LESS_THAN:
                return isLessHits(value, alarm, clusterId);
            default:
                return false;
        }
    }

    private boolean isEqualsHits(double value, Alarm alarm, long clusterId) {
        return isComparisonHit(value == alarm.getThreshold(), alarm, ComparisonOperator.EQUALS, clusterId);
    }

    private boolean isGreaterOrEqualHits(double value, Alarm alarm, long clusterId) {
        return isComparisonHit(value >= alarm.getThreshold(), alarm, ComparisonOperator.GREATER_OR_EQUAL_THAN, clusterId);
    }

    private boolean isGreaterHits(double value, Alarm alarm, long clusterId) {
        return isComparisonHit(value > alarm.getThreshold(), alarm, ComparisonOperator.GREATER_THAN, clusterId);
    }

    private boolean isLessOrEqualHits(double value, Alarm alarm, long clusterId) {
        return isComparisonHit(value <= alarm.getThreshold(), alarm, ComparisonOperator.LESS_OR_EQUAL_THAN, clusterId);
    }

    private boolean isLessHits(double value, Alarm alarm, long clusterId) {
        return isComparisonHit(value < alarm.getThreshold(), alarm, ComparisonOperator.LESS_THAN, clusterId);
    }

    private boolean isComparisonHit(boolean valueHit, Alarm alarm, ComparisonOperator operator, long clusterId) {
        boolean result = false;
        String alarmName = alarm.getName();
        if (valueHit) {
            LOGGER.info(clusterId, "{} comparison hit for alarm: {}", operator, alarmName);
            result = setAndCheckTime(alarm, clusterId);
        } else {
            LOGGER.info(clusterId, "{} comparison failed for alarm: {}", operator, alarmName);
            reset(alarm);
        }
        return result;
    }

    private boolean setAndCheckTime(Alarm alarm, long clusterId) {
        boolean result = false;
        String alarmName = alarm.getName();
        long hitsSince = alarm.getAlarmHitsSince();
        if (hitsSince == 0) {
            LOGGER.info(clusterId, "Counter starts until hit for alarm: {}", alarmName);
            setCurrentTime(alarm);
        } else {
            long elapsedTime = System.currentTimeMillis() - hitsSince;
            result = elapsedTime > (alarm.getPeriod() * ClusterUtils.MIN_IN_MS);
            LOGGER.info(clusterId, "Alarm: {} stands since {}ms", alarmName, elapsedTime);
            if (result) {
                LOGGER.info(clusterId, "Alarm: {} HIT", alarmName);
            }
        }
        return result;
    }

    private void reset(Alarm alarm) {
        alarm.reset();
    }

    private void setCurrentTime(Alarm alarm) {
        alarm.setAlarmHitsSince(System.currentTimeMillis());
    }

    private void handleScaling(Cluster cluster, Alarm alarm) {
        ScalingPolicy scalingPolicy = alarm.getScalingPolicy();
        if (scalingPolicy != null) {
            scalingService.scale(cluster, scalingPolicy);
        }
    }

    private void handleNotifications(Cluster cluster, Alarm alarm) {
        if (!alarm.isNotificationSent()) {
            for (Notification notification : alarm.getNotifications()) {
                notificationService.sendNotification(cluster, alarm, notification);
            }
            alarm.setNotificationSent(true);
        }
    }

}