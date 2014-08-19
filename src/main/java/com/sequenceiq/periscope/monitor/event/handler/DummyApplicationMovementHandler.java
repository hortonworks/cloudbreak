package com.sequenceiq.periscope.monitor.event.handler;

import static com.sequenceiq.periscope.utils.ClusterUtils.computeFreeQueueResourceCapacity;
import static com.sequenceiq.periscope.utils.ClusterUtils.getAllQueueInfo;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.CapacitySchedulerQueueInfo;
import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.SchedulerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.model.Cluster;
import com.sequenceiq.periscope.model.Priority;
import com.sequenceiq.periscope.model.SchedulerApplication;
import com.sequenceiq.periscope.monitor.event.ApplicationUpdateEvent;
import com.sequenceiq.periscope.service.ClusterNotFoundException;
import com.sequenceiq.periscope.service.ClusterService;

@Component
public class DummyApplicationMovementHandler implements ApplicationListener<ApplicationUpdateEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DummyApplicationMovementHandler.class);

    @Autowired
    private ClusterService clusterService;

    @Override
    public void onApplicationEvent(ApplicationUpdateEvent event) {
        List<ApplicationReport> appReports = event.getReports();
        SchedulerInfo schedulerInfo = event.getSchedulerInfo();

        List<CapacitySchedulerQueueInfo> allQueueInfo = getAllQueueInfo(schedulerInfo);
        Cluster cluster;
        try {
            cluster = clusterService.get(event.getClusterId());
        } catch (ClusterNotFoundException e) {
            LOGGER.error("Cluster not found and cannot move applications, id: " + event.getClusterId(), e);
            return;
        }

        Map<Priority, Map<ApplicationId, SchedulerApplication>> apps = cluster.getApplicationsPriorityOrder();
        Set<ApplicationId> activeApps = new HashSet<>();
        for (ApplicationReport report : appReports) {
            ApplicationId id = report.getApplicationId();
            activeApps.add(id);
            SchedulerApplication application = addApplicationIfAbsent(cluster, report);
            application.update(report);
            if (cluster.isAppMovementAllowed() && allQueueInfo.size() > 1) {
                apps = cluster.getApplicationsPriorityOrder();
                if (isApplicationHighPriority(apps, id)) {
                    LOGGER.info("Try to move high priority app {}", id);
                    CapacitySchedulerQueueInfo queue = getQueueWithMostAvailableCapacity(cluster, allQueueInfo);
                    if (!application.isMoved()) {
                        float availableMemory = computeFreeQueueResourceCapacity(cluster, queue);
                        int usedMemory = report.getApplicationResourceUsageReport().getUsedResources().getMemory();
                        if (availableMemory - usedMemory > 0) {
                            try {
                                cluster.getYarnClient().moveApplicationAcrossQueues(id, queue.getQueueName());
                                application.setMoved(true);
                                LOGGER.info("Application {} moved to queue {}", id.toString(), queue.getQueueName());
                                break;
                            } catch (IOException | YarnException e) {
                                LOGGER.error("Error moving {} to {}", id.toString(), queue.getQueueName(), e);
                            }
                        }
                    }
                    LOGGER.info("Queue with most available resource : {}", queue.getQueueName());
                }
            }
        }
        removeApplicationIfFinished(cluster, activeApps, apps);
    }

    private CapacitySchedulerQueueInfo getQueueWithMostAvailableCapacity(Cluster cluster, List<CapacitySchedulerQueueInfo> allQueueInfo) {
        CapacitySchedulerQueueInfo result = allQueueInfo.get(0);
        int numQueues = allQueueInfo.size();
        for (int i = 1; i < numQueues; i++) {
            CapacitySchedulerQueueInfo queue = allQueueInfo.get(i);
            if (computeFreeQueueResourceCapacity(cluster, queue) > computeFreeQueueResourceCapacity(cluster, result)) {
                result = queue;
            }
        }
        return result;
    }

    private boolean isApplicationHighPriority(Map<Priority, Map<ApplicationId, SchedulerApplication>> apps, ApplicationId id) {
        Map<ApplicationId, SchedulerApplication> high = apps.get(Priority.HIGH);
        return high != null && high.containsKey(id);
    }

    private void removeApplicationIfFinished(Cluster cluster, Set<ApplicationId> activeApps,
            Map<Priority, Map<ApplicationId, SchedulerApplication>> apps) {
        for (Priority priority : apps.keySet()) {
            for (ApplicationId id : apps.get(priority).keySet()) {
                if (!activeApps.contains(id)) {
                    cluster.removeApplication(id);
                }
            }
        }
    }

    private SchedulerApplication addApplicationIfAbsent(Cluster cluster, ApplicationReport appReport) {
        ApplicationId appId = appReport.getApplicationId();
        SchedulerApplication application = cluster.getApplication(appId);
        if (application == null) {
            application = cluster.addApplication(appReport);
        }
        return application;
    }

}
