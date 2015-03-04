package com.sequenceiq.periscope.monitor.request;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.SchedulerInfo;
import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.SchedulerTypeInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestOperations;

import com.sequenceiq.periscope.log.Logger;
import com.sequenceiq.periscope.log.PeriscopeLoggerFactory;
import com.sequenceiq.periscope.model.QueueAppUpdate;
import com.sequenceiq.periscope.monitor.event.ApplicationUpdateEvent;
import com.sequenceiq.periscope.monitor.event.UpdateFailedEvent;

@Component("ApplicationMetricRequest")
@Scope("prototype")
public class ApplicationMetricRequest extends AbstractEventPublisher implements Request {

    private static final Logger LOGGER = PeriscopeLoggerFactory.getLogger(ApplicationMetricRequest.class);
    private static final String WS_URL = "/ws/v1/cluster/scheduler";

    @Autowired
    private RestOperations restOperations;
    private long clusterId;
    private String yarnWebAddress;
    private YarnClient yarnClient;

    @Override
    public void setContext(Map<String, Object> context) {
        this.clusterId = (long) context.get(RequestContext.CLUSTER_ID.name());
        this.yarnClient = (YarnClient) context.get(RequestContext.YARN_CLIENT.name());
        this.yarnWebAddress = (String) context.get(RequestContext.YARN_RM_WEB_ADDRESS.name());
    }

    @Override
    public void run() {
        try {
            List<ApplicationReport> applicationReport = getApplicationReport();
            SchedulerInfo schedulerInfo = getSchedulerInfo();
            QueueAppUpdate appUpdate = new QueueAppUpdate(applicationReport, schedulerInfo);
            publishEvent(new ApplicationUpdateEvent(clusterId, appUpdate));
        } catch (Exception e) {
            LOGGER.error(clusterId, "Error occurred during application update", e);
            publishEvent(new UpdateFailedEvent(clusterId));
        }
    }

    private List<ApplicationReport> getApplicationReport() throws YarnException, IOException {
        // TODO should check for proper state to start with
        EnumSet<YarnApplicationState> states = EnumSet.of(YarnApplicationState.RUNNING);
        return yarnClient.getApplications(states);
    }

    private SchedulerInfo getSchedulerInfo() throws NoSuchFieldException, IllegalAccessException {
        String url = "http://" + yarnWebAddress + WS_URL;
        SchedulerTypeInfo response = restOperations.getForObject(url, SchedulerTypeInfo.class);
        // TODO https://issues.apache.org/jira/browse/YARN-2280
        Field field = response.getClass().getDeclaredField("schedulerInfo");
        field.setAccessible(true);
        return (SchedulerInfo) field.get(response);
    }
}
