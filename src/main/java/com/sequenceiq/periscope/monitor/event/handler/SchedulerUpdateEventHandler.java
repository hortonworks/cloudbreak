package com.sequenceiq.periscope.monitor.event.handler;

import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.CapacitySchedulerInfo;
import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.CapacitySchedulerQueueInfo;
import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.CapacitySchedulerQueueInfoList;
import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.SchedulerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.monitor.event.SchedulerUpdateEvent;

@Component
public class SchedulerUpdateEventHandler implements ApplicationListener<SchedulerUpdateEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchedulerUpdateEventHandler.class);

    @Override
    public void onApplicationEvent(SchedulerUpdateEvent event) {
        SchedulerInfo schedulerInfo = event.getSchedulerInfo();
        if (schedulerInfo instanceof CapacitySchedulerInfo) {
            printCSSchedulerMetrics((CapacitySchedulerInfo) schedulerInfo);
        }
    }

    private void printCSSchedulerMetrics(CapacitySchedulerInfo schedulerInfo) {
        printCSSchedulerMetrics(schedulerInfo.getQueues());
    }

    private void printCSSchedulerMetrics(CapacitySchedulerQueueInfoList infoList) {
        if (infoList != null && infoList.getQueueInfoList() != null) {
            for (CapacitySchedulerQueueInfo info : infoList.getQueueInfoList()) {
                StringBuilder sb = new StringBuilder();

                sb.append("\nQueue name: ").append(info.getQueueName());
                sb.append("\ncapacity: ").append(info.getCapacity());
                sb.append("\nmax capacity: ").append(info.getMaxCapacity());
                sb.append("\nabsolute capacity: ").append(info.getAbsoluteCapacity());
                sb.append("\nabsolute max capacity: ").append(info.getAbsoluteMaxCapacity());
                sb.append("\nused capacity: ").append(info.getUsedCapacity());
                sb.append("\nabsolute used capacity: ").append(info.getAbsoluteUsedCapacity());
                sb.append("\nnumber of apps: ").append(info.getNumApplications());
                sb.append("\nused resources: ").append(info.getResourcesUsed());
                sb.append("\nused capacity: ").append(info.getUsedCapacity());

                LOGGER.info(sb.toString());
                printCSSchedulerMetrics(info.getQueues());
            }
        }
    }
}
