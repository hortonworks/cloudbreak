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
import com.sequenceiq.periscope.registry.ClusterRegistration;

@Component
@Scope("prototype")
public class ApplicationReportUpdateRequest extends AbstractEventPublisher implements Runnable, EventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationReportUpdateRequest.class);

    private ClusterRegistration clusterRegistration;

    public ApplicationReportUpdateRequest(ClusterRegistration clusterRegistration) {
        this.clusterRegistration = clusterRegistration;
    }

    @Override
    public void run() {
        try {
            EnumSet<YarnApplicationState> states = EnumSet.of(YarnApplicationState.RUNNING);
            List<ApplicationReport> applications = clusterRegistration.getYarnClient().getApplications(states);
            publishEvent(new ApplicationReportUpdateEvent(clusterRegistration.getClusterId(), applications));
        } catch (IOException | YarnException e) {
            LOGGER.error("Error occurred during application report update from cluster {}", clusterRegistration.getClusterId(), e);
            publishEvent(new UpdateFailedEvent(clusterRegistration.getClusterId()));
        }
    }
}
