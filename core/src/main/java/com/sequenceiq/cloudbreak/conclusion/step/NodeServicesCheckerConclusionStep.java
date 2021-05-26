package com.sequenceiq.cloudbreak.conclusion.step;

import static com.cloudera.thunderhead.telemetry.nodestatus.NodeStatusProto.NodeStatus;
import static com.cloudera.thunderhead.telemetry.nodestatus.NodeStatusProto.NodeStatusReport;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.telemetry.nodestatus.NodeStatusProto.HealthStatus;
import com.cloudera.thunderhead.telemetry.nodestatus.NodeStatusProto.ServiceStatus;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.client.RPCResponse;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.node.status.NodeStatusService;

@Component
public class NodeServicesCheckerConclusionStep extends ConclusionStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(NodeServicesCheckerConclusionStep.class);

    private static final String SALT_BOOTSTRAP = "salt-bootstrap";

    @Inject
    private NodeStatusService nodeStatusService;

    @Override
    public Conclusion check(Long resourceId) {
        RPCResponse<NodeStatusReport> servicesReport;
        try {
            servicesReport = nodeStatusService.getServicesReport(resourceId);
        } catch (CloudbreakServiceException e) {
            LOGGER.debug("Node services report failed, error: {}", e.getMessage());
            return succeeded();
        }
        if (servicesReport.getResult() == null) {
            String nodeStatusResult = servicesReport.getFirstTextMessage();
            LOGGER.info("Node services report result was null, original message: {}", nodeStatusResult);
            return succeeded();
        }

        Multimap<String, String> nodesWithUnhealthyServices = collectNodesWithUnhealthyServices(servicesReport);
        if (nodesWithUnhealthyServices.isEmpty()) {
            return succeeded();
        } else {
            String conclusion = String.format("There are unhealthy services on nodes: %s. " +
                            "Please check the instances on your cloud provider for further details.", nodesWithUnhealthyServices.keySet());
            String details = String.format("There are unhealthy services on nodes: %s", nodesWithUnhealthyServices);
            LOGGER.warn(details);
            return failed(conclusion, details);
        }
    }

    private Multimap<String, String> collectNodesWithUnhealthyServices(RPCResponse<NodeStatusReport> servicesReport) {
        Multimap<String, String> nodesWithUnhealthyServices = HashMultimap.create();
        for (NodeStatus nodeStatus : servicesReport.getResult().getNodesList()) {
            for (ServiceStatus serviceStatus : nodeStatus.getServicesDetails().getInfraServicesList()) {
                if (SALT_BOOTSTRAP.equals(serviceStatus.getName())) {
                    if (HealthStatus.NOK.equals(serviceStatus.getStatus())) {
                        nodesWithUnhealthyServices.put(nodeStatus.getStatusDetails().getHost(), serviceStatus.getName());
                    }
                }
            }
        }
        return nodesWithUnhealthyServices;
    }
}
