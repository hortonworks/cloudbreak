package com.sequenceiq.periscope.monitor.request;

import java.util.Map;

import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.ClusterMetricsInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestOperations;

import com.sequenceiq.periscope.log.Logger;
import com.sequenceiq.periscope.log.PeriscopeLoggerFactory;
import com.sequenceiq.periscope.monitor.event.YarnMetricUpdateEvent;
import com.sequenceiq.periscope.monitor.event.EventWrapper;
import com.sequenceiq.periscope.monitor.event.UpdateFailedEvent;

@Component("YarnMetricRequest")
@Scope("prototype")
public class YarnMetricRequest extends AbstractEventPublisher implements Request {

    private static final Logger LOGGER = PeriscopeLoggerFactory.getLogger(YarnMetricRequest.class);
    private static final String WS_URL = "/ws/v1/cluster/metrics";

    @Autowired
    private RestOperations restOperations;
    private long clusterId;
    private String yarnWebAddress;

    @Override
    public void setContext(Map<String, Object> context) {
        this.clusterId = (long) context.get(RequestContext.CLUSTER_ID.name());
        this.yarnWebAddress = (String) context.get(RequestContext.YARN_RM_WEB_ADDRESS.name());
    }

    @Override
    public void run() {
        try {
            String url = "http://" + yarnWebAddress + WS_URL;
            ClusterMetricsInfo response = restOperations.getForObject(url, ClusterMetricsInfo.class);
            publishEvent(new EventWrapper(new YarnMetricUpdateEvent(response, clusterId)));
        } catch (Exception e) {
            LOGGER.error(clusterId, "Error updating the cluster metrics via WebService", e);
            publishEvent(new UpdateFailedEvent(clusterId));
        }
    }
}
