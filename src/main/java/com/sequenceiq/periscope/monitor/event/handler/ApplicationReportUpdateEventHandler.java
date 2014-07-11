package com.sequenceiq.periscope.monitor.event.handler;

import java.util.Date;

import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.api.records.ApplicationResourceUsageReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.monitor.event.ApplicationReportUpdateEvent;

@Component
public class ApplicationReportUpdateEventHandler implements ApplicationListener<ApplicationReportUpdateEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationReportUpdateEventHandler.class);

    @Override
    public void onApplicationEvent(ApplicationReportUpdateEvent event) {
        for (ApplicationReport report : event.getReports()) {
            printApplicationReport(report);
        }
    }

    private void printApplicationReport(ApplicationReport report) {
        StringBuilder sb = new StringBuilder();

        sb.append("\nApplication: ").append(report.getApplicationId());
        sb.append("\ntype: ").append(report.getApplicationType());
        sb.append("\nqueue: ").append(report.getQueue());
        sb.append("\nstart time: ").append(new Date(report.getStartTime()));
        sb.append("\nprogress: ").append(report.getProgress());
        sb.append("\nuser: ").append(report.getUser());

        ApplicationResourceUsageReport usage = report.getApplicationResourceUsageReport();
        sb.append("\nreserved containers: ").append(usage.getNumReservedContainers());
        sb.append("\nreserved resources: ").append(usage.getReservedResources());
        sb.append("\nneeded resource: ").append(usage.getNeededResources());
        sb.append("\nused containers: ").append(usage.getNumUsedContainers());
        sb.append("\nused resources").append(usage.getUsedResources());

        LOGGER.info(sb.toString());
    }

}
