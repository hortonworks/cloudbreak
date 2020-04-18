package com.sequenceiq.periscope.monitor.evaluator.ambari;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.periscope.aspects.RequestLogging;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ClusterManagerVariant;
import com.sequenceiq.periscope.monitor.evaluator.ClusterManagerSpecificHostHealthEvaluator;
import com.sequenceiq.periscope.monitor.evaluator.EventPublisher;
import com.sequenceiq.periscope.monitor.event.UpdateFailedEvent;
import com.sequenceiq.periscope.service.AmbariClientProvider;

@Component("AmbariAgentHealthEvaluator")
@Scope("prototype")
public class AmbariAgentHealthEvaluator implements ClusterManagerSpecificHostHealthEvaluator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariAgentHealthEvaluator.class);

    private static final String ALERT_STATE = "state";

    private static final String CRITICAL_ALERT_STATE = "CRITICAL";

    private static final String AMBARI_AGENT_HEARTBEAT = "Ambari Agent Heartbeat";

    private static final String AMBARI_AGENT_HEARTBEAT_DEF_NAME = "ambari_server_agent_heartbeat";

    private static final String HOST_NAME = "host_name";

    @Inject
    private AmbariClientProvider ambariClientProvider;

    @Inject
    private RequestLogging ambariRequestLogging;

    @Inject
    private EventPublisher eventPublisher;

    @Override
    public ClusterManagerVariant getSupportedClusterManagerVariant() {
        return ClusterManagerVariant.AMBARI;
    }

    @Override
    public List<String> determineHostnamesToRecover(Cluster cluster) {
        long start = System.currentTimeMillis();
        Long clusterId = cluster.getId();
        try {
            MDCBuilder.buildMdcContext(cluster);
            LOGGER.debug("Checking '{}' alerts for cluster {}.", AMBARI_AGENT_HEARTBEAT, clusterId);
            AmbariClient ambariClient = ambariClientProvider.createAmbariClient(cluster);
            return ambariRequestLogging.logResponseTime(() -> ambariClient.getAlert(AMBARI_AGENT_HEARTBEAT_DEF_NAME), "alert")
                    .stream()
                    .filter(history -> {
                        String currentState = (String) history.get(ALERT_STATE);
                        return isAlertStateMet(currentState);
                    })
                    .peek(history -> {
                        String currentState = (String) history.get(ALERT_STATE);
                        String hostName = (String) history.get(HOST_NAME);
                        LOGGER.debug("Alert: {} is in '{}' state for host '{}'.", AMBARI_AGENT_HEARTBEAT, currentState, hostName);
                    })
                    .map(history -> (String) history.get(HOST_NAME))
                    .peek(hn -> LOGGER.debug("Host to recover: {}", hn))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            LOGGER.info(String.format("Failed to retrieve '%s' alerts. Original message: %s", AMBARI_AGENT_HEARTBEAT, e.getMessage()));
            eventPublisher.publishEvent(new UpdateFailedEvent(clusterId));
        } finally {
            LOGGER.debug("Finished {} for cluster {} in {} ms", AMBARI_AGENT_HEARTBEAT, clusterId, System.currentTimeMillis() - start);
        }
        return List.of();
    }

    private boolean isAlertStateMet(String currentState) {
        return currentState.equalsIgnoreCase(CRITICAL_ALERT_STATE);
    }
}
