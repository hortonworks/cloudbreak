package com.sequenceiq.periscope.monitor.request;

import java.lang.reflect.Field;

import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.SchedulerInfo;
import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.SchedulerTypeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestOperations;

import com.sequenceiq.periscope.monitor.event.SchedulerUpdateEvent;
import com.sequenceiq.periscope.monitor.event.UpdateFailedEvent;
import com.sequenceiq.periscope.registry.ClusterRegistration;
import com.sequenceiq.periscope.service.configuration.ConfigParam;

@Component("SchedulerUpdateRequest")
@Scope("prototype")
public class SchedulerUpdateRequest extends AbstractEventPublisher implements Runnable, EventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchedulerUpdateRequest.class);
    private static final String WS_URL = "/ws/v1/cluster/scheduler";

    @Autowired
    private RestOperations restOperations;
    private ClusterRegistration clusterRegistration;

    public SchedulerUpdateRequest(ClusterRegistration clusterRegistration) {
        this.clusterRegistration = clusterRegistration;
    }

    @Override
    public void run() {
        try {
            String rmAddress = clusterRegistration.getConfigValue(ConfigParam.YARN_RM_WEB_ADDRESS, "");
            String url = "http://" + rmAddress + WS_URL;
            SchedulerTypeInfo response = restOperations.getForObject(url, SchedulerTypeInfo.class);
            // TODO https://issues.apache.org/jira/browse/YARN-2280
            Field field = response.getClass().getDeclaredField("schedulerInfo");
            field.setAccessible(true);
            SchedulerInfo schedulerInfo = (SchedulerInfo) field.get(response);
            publishEvent(new SchedulerUpdateEvent(schedulerInfo, clusterRegistration.getClusterId()));
        } catch (Exception e) {
            LOGGER.error("Error updating the scheduler info from the WS {}", clusterRegistration.getClusterId(), e);
            publishEvent(new UpdateFailedEvent(clusterRegistration.getClusterId()));
        }
    }
}
