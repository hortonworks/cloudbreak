package com.sequenceiq.periscope.monitor.evaluator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.api.model.FailureReport;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.periscope.aspects.AmbariRequestLogging;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.monitor.context.ClusterIdEvaluatorContext;
import com.sequenceiq.periscope.monitor.context.EvaluatorContext;
import com.sequenceiq.periscope.monitor.event.UpdateFailedEvent;
import com.sequenceiq.periscope.service.AmbariClientProvider;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.configuration.CloudbreakClientConfiguration;

@Component("AmbariAgentHealthEvaluator")
@Scope("prototype")
public class AmbariAgentHealthEvaluator extends EvaluatorExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariAgentHealthEvaluator.class);

    private static final String ALERT_STATE = "state";

    private static final String ALERT_TS = "timestamp";

    private static final String CRITICAL_ALERT_STATE = "CRITICAL";

    private static final String AMBARI_AGENT_HEARTBEAT = "Ambari Agent Heartbeat";

    private static final String AMBARI_AGENT_HEARTBEAT_DEF_NAME = "ambari_server_agent_heartbeat";

    private static final String HOST_NAME = "host_name";

    private static final String EVALUATOR_NAME = AmbariAgentHealthEvaluator.class.getName();

    @Inject
    private ClusterService clusterService;

    @Inject
    private AmbariClientProvider ambariClientProvider;

    @Inject
    private CloudbreakClientConfiguration cloudbreakClientConfiguration;

    @Inject
    private AmbariRequestLogging ambariRequestLogging;

    @Inject
    private EventPublisher eventPublisher;

    private long clusterId;

    @Override
    public void setContext(EvaluatorContext context) {
        clusterId = (long) context.getData();
    }

    @Override
    @Nonnull
    public EvaluatorContext getContext() {
        return new ClusterIdEvaluatorContext(clusterId);
    }

    @Override
    public String getName() {
        return EVALUATOR_NAME;
    }

    @Override
    public void execute() {
        long start = System.currentTimeMillis();
        try {
            Cluster cluster = clusterService.findById(clusterId);
            MDCBuilder.buildMdcContext(cluster);
            LOGGER.info("Checking '{}' alerts for cluster {}.", AMBARI_AGENT_HEARTBEAT, cluster.getId());
            AmbariClient ambariClient = ambariClientProvider.createAmbariClient(cluster);
            List<Map<String, Object>> alertHistory = ambariRequestLogging.logging(() -> ambariClient.getAlert(AMBARI_AGENT_HEARTBEAT_DEF_NAME), "alert");
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
                    hostNamesToRecover.forEach(hn -> LOGGER.info("Host to recover: {}", hn));
                    CloudbreakClient cbClient = cloudbreakClientConfiguration.cloudbreakClient();
                    FailureReport failureReport = new FailureReport();
                    failureReport.setFailedNodes(hostNamesToRecover);
                    cbClient.autoscaleEndpoint().failureReport(cluster.getStackId(), failureReport);
                }
            }
        } catch (Exception e) {
            LOGGER.warn(String.format("Failed to retrieve '%s' alerts. Original message: %s", AMBARI_AGENT_HEARTBEAT, e.getMessage()));
            eventPublisher.publishEvent(new UpdateFailedEvent(clusterId));
        } finally {
            LOGGER.info("Finished {} for cluster {} in {} ms", AMBARI_AGENT_HEARTBEAT, clusterId, System.currentTimeMillis() - start);
        }
    }

    private boolean isAlertStateMet(String currentState) {
        return currentState.equalsIgnoreCase(CRITICAL_ALERT_STATE);
    }
}
