package com.sequenceiq.periscope.monitor.request;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;

import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.monitor.event.ApplicationReportUpdateEvent;
import com.sequenceiq.periscope.monitor.event.UpdateFailedEvent;
import com.sequenceiq.periscope.registry.Cluster;

@Component("ApplicationReportUpdateRequest")
@Scope("prototype")
public class ApplicationReportUpdateRequest extends AbstractEventPublisher implements EventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationReportUpdateRequest.class);
    private final Cluster cluster;

    public ApplicationReportUpdateRequest(Cluster cluster) {
        this.cluster = cluster;
    }

    @Override
    public void run() {
        try {
            EnumSet<YarnApplicationState> states = EnumSet.of(YarnApplicationState.RUNNING);
            List<ApplicationReport> applications = cluster.getYarnClient().getApplications(states);
            publishEvent(new ApplicationReportUpdateEvent(cluster.getClusterId(), applications));
        } catch (IOException | YarnException e) {
            LOGGER.error("Error occurred during application report update from cluster {}", cluster.getClusterId(), e);
            publishEvent(new UpdateFailedEvent(cluster.getClusterId()));
        }
    }
}
