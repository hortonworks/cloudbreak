package com.sequenceiq.periscope.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.CapacitySchedulerInfo;
import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.CapacitySchedulerQueueInfo;
import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.CapacitySchedulerQueueInfoList;
import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.ClusterMetricsInfo;
import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.SchedulerInfo;

import com.sequenceiq.periscope.model.Cluster;

public final class ClusterUtils {

    public static final int MAX_CAPACITY = 100;
    public static final int MIN_IN_MS = 1000 * 60;

    private ClusterUtils() {
        throw new IllegalStateException();
    }

    public static double computeFreeClusterResourceRate(ClusterMetricsInfo metrics) {
        return (double) metrics.getAvailableMB() / (double) metrics.getTotalMB();
    }

    public static float computeMaxQueueResourceCapacity(Cluster cluster, CapacitySchedulerQueueInfo queue) {
        return cluster.getTotalMB() * (queue.getAbsoluteMaxCapacity() / MAX_CAPACITY);
    }

    public static float computeFreeQueueResourceCapacity(Cluster cluster, CapacitySchedulerQueueInfo queue) {
        return computeMaxQueueResourceCapacity(cluster, queue) - queue.getResourcesUsed().getMemory();
    }

    public static List<CapacitySchedulerQueueInfo> getAllQueueInfo(SchedulerInfo schedulerInfo) {
        List<CapacitySchedulerQueueInfo> queueInfoList = new ArrayList<>();
        if (schedulerInfo instanceof CapacitySchedulerInfo) {
            addQueueInfo(queueInfoList, ((CapacitySchedulerInfo) schedulerInfo).getQueues());
        }
        return queueInfoList;
    }

    private static void addQueueInfo(List<CapacitySchedulerQueueInfo> queueInfoList, CapacitySchedulerQueueInfoList queues) {
        if (queues != null && queues.getQueueInfoList() != null) {
            for (CapacitySchedulerQueueInfo info : queues.getQueueInfoList()) {
                queueInfoList.add(info);
                addQueueInfo(queueInfoList, info.getQueues());
            }
        }
    }
}
