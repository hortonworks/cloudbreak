package com.sequenceiq.cloudbreak.conclusion.step;

import static com.cloudera.thunderhead.telemetry.nodestatus.NodeStatusProto.NodeStatus;
import static com.cloudera.thunderhead.telemetry.nodestatus.NodeStatusProto.NodeStatusReport;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.telemetry.nodestatus.NodeStatusProto;
import com.cloudera.thunderhead.telemetry.nodestatus.NodeStatusProto.ServiceStatus;
import com.sequenceiq.cloudbreak.client.RPCResponse;
import com.sequenceiq.cloudbreak.node.status.NodeStatusService;

@Component
public class SaltBootstrapCheckerConclusionStep extends ConclusionStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltBootstrapCheckerConclusionStep.class);

    private static final String SALT_BOOTSTRAP = "salt-bootstrap";

    @Inject
    private NodeStatusService nodeStatusService;

    @Override
    public Conclusion check(Long resourceId) {
        RPCResponse<NodeStatusReport> servicesReport = nodeStatusService.getServicesReport(resourceId);
        if (servicesReport.getResult() == null) {
            String nodeStatusResult = servicesReport.getFirstTextMessage();
            LOGGER.info("Node status report result was null, original message: {}", nodeStatusResult);
            return succeeded();
        }

        Set<String> unreachableNodes = filterUnreachableNodes(servicesReport);
        if (unreachableNodes.isEmpty()) {
            return succeeded();
        } else {
            String conclusion = String.format("Unreachable nodes: %s. Please check the instances on your cloud provider for further details.",
                    unreachableNodes);
            String details = String.format("Unreachable salt bootstrap nodes: %s", unreachableNodes);
            LOGGER.error(details);
            return failed(conclusion, details);
        }
    }

    private Set<String> filterUnreachableNodes(RPCResponse<NodeStatusReport> servicesReport) {
        Set<String> unreachableNodes = new HashSet<>();
        for (NodeStatus nodeStatus : servicesReport.getResult().getNodesList()) {
            for (ServiceStatus serviceStatus : nodeStatus.getServicesDetails().getInfraServicesList()) {
                if (SALT_BOOTSTRAP.equals(serviceStatus.getName())) {
                    if (!NodeStatusProto.HealthStatus.OK.equals(serviceStatus.getStatus())) {
                        unreachableNodes.add(nodeStatus.getStatusDetails().getHost());
                    }
                }
            }
        }
        return unreachableNodes;
    }
}
