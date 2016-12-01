package com.sequenceiq.periscope.monitor.evaluator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.log.MDCBuilder;
import com.sequenceiq.periscope.monitor.event.UpdateFailedEvent;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.configuration.CloudbreakClientConfiguration;
import com.sequenceiq.periscope.utils.AmbariClientProvider;

@Component("AmbariAgentHealthEvaluator")
@Scope("prototype")
public class AmbariAgentHealthEvaluator extends AbstractEventPublisher implements EvaluatorExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariAgentHealthEvaluator.class);

    private static final String ALERT_STATE = "state";

    private static final String ALERT_TS = "timestamp";

    private static final String CRITICAL_ALERT_STATE = "CRITICAL";

    private static final String AMBARI_AGENT_HEARTBEAT = "Ambari Agent Heartbeat";

    private static final String AMBARI_AGENT_HEARTBEAT_DEF_NAME = "ambari_server_agent_heartbeat";

    private static final String HOST_NAME = "host_name";

    @Inject
    private ClusterService clusterService;

    @Inject
    private AmbariClientProvider ambariClientProvider;

    @Inject
    private CloudbreakClientConfiguration cloudbreakClientConfiguration;

    private long clusterId;

    @Override
    public void setContext(Map<String, Object> context) {
        this.clusterId = (long) context.get(EvaluatorContext.CLUSTER_ID.name());
    }

    @Override
    public void run() {
        LOGGER.info("Checking '{}' alerts.", AMBARI_AGENT_HEARTBEAT);
        Cluster cluster = clusterService.find(clusterId);
        MDCBuilder.buildMdcContext(cluster);
        try {
            AmbariClient ambariClient = ambariClientProvider.createAmbariClient(cluster);
            List<Map<String, Object>> alertHistory = ambariClient.getAlert(AMBARI_AGENT_HEARTBEAT_DEF_NAME);
            if (!alertHistory.isEmpty()) {
                List<String> hostNamesToRecover = new ArrayList<>();
                for (Map<String, Object> history : alertHistory) {
                    String currentState = (String) history.get(ALERT_STATE);
                    if (isAlertStateMet(currentState)) {
                        String hostName = (String) history.get(HOST_NAME);
                        hostNamesToRecover.add(hostName);
                        LOGGER.info("Alert: {} is in '{}' state for host '{}'.", AMBARI_AGENT_HEARTBEAT, currentState, hostName);
                    }
                }
                if (!hostNamesToRecover.isEmpty()) {
                    hostNamesToRecover.stream().forEach(hn -> LOGGER.info("Host to recover: {}", hn));
                    CloudbreakClient cbClient = cloudbreakClientConfiguration.cloudbreakClient();
                    //TODO post the recovery candidates to Cloudbreak stackendpoint
                }
            }
        } catch (Exception e) {
            LOGGER.warn(String.format("Failed to retrieve '%s' alerts.", AMBARI_AGENT_HEARTBEAT), e);
            publishEvent(new UpdateFailedEvent(clusterId));
        }
    }

    private boolean isAlertStateMet(String currentState) {
        return currentState.equalsIgnoreCase(CRITICAL_ALERT_STATE);
    }
}
