package com.sequenceiq.cloudbreak.conclusion.step;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.telemetry.nodestatus.NodeStatusProto.HealthStatus;
import com.cloudera.thunderhead.telemetry.nodestatus.NodeStatusProto.NetworkDetails;
import com.cloudera.thunderhead.telemetry.nodestatus.NodeStatusProto.NodeStatus;
import com.cloudera.thunderhead.telemetry.nodestatus.NodeStatusProto.NodeStatusReport;
import com.sequenceiq.cloudbreak.client.RPCResponse;
import com.sequenceiq.cloudbreak.node.status.NodeStatusService;

@Component
public class NetworkCheckerConclusionStep extends ConclusionStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkCheckerConclusionStep.class);

    @Inject
    private NodeStatusService nodeStatusService;

    @Override
    public Conclusion check(Long resourceId) {
        RPCResponse<NodeStatusReport> networkReport;
        try {
            networkReport = nodeStatusService.getNetworkReport(resourceId);
        } catch (Exception e) {
            LOGGER.debug("Network report failed, error: {}", e.getMessage());
            return succeeded();
        }
        if (networkReport.getResult() == null) {
            String nodeStatusResult = networkReport.getFirstTextMessage();
            LOGGER.info("Network report result was null, original message: {}", nodeStatusResult);
            return succeeded();
        }

        List<String> networkFailures = new ArrayList<>();
        List<String> networkFailureDetails = new ArrayList<>();
        checkNetworkFailures(networkReport, networkFailures, networkFailureDetails);

        if (networkFailures.isEmpty()) {
            return succeeded();
        } else {
            return failed(networkFailures.toString(), networkFailureDetails.toString());
        }
    }

    private void checkNetworkFailures(RPCResponse<NodeStatusReport> networkReport, List<String> networkFailures, List<String> networkFailureDetails) {
        for (NodeStatus nodeStatus : networkReport.getResult().getNodesList()) {
            NetworkDetails networkDetails = nodeStatus.getNetworkDetails();
            String host = nodeStatus.getStatusDetails().getHost();
            if (networkDetails.getCcmEnabled() && HealthStatus.NOK.equals(networkDetails.getCcmAccessible())) {
                networkFailures.add("CCM is not accessible from node " + host + ". Please check network settings!");
                String details = String.format("CCM health status is %s for node %s", networkDetails.getCcmAccessible(), host);
                networkFailureDetails.add(details);
                LOGGER.warn(details);
            }
            if (HealthStatus.NOK.equals(networkDetails.getClouderaComAccessible())) {
                networkFailures.add("Cloudera.com is not accessible from node: " + host + ". Please check network settings!");
                String details = String.format("Cloudera.com accessibility status is %s for node %s", networkDetails.getClouderaComAccessible(), host);
                networkFailureDetails.add(details);
                LOGGER.warn(details);
            }
            if (networkDetails.getNeighbourScan() && HealthStatus.NOK.equals(networkDetails.getAnyNeighboursAccessible())) {
                networkFailures.add("Node " + host + " cannot reach any neighbour nodes. Please check nodes and network settings!");
                String details = String.format("Neighbours accessibility status is %s for node %s", networkDetails.getAnyNeighboursAccessible(), host);
                networkFailureDetails.add(details);
                LOGGER.warn(details);
            }
        }
    }
}
