package com.sequenceiq.cloudbreak.conclusion.step;

import static com.cloudera.thunderhead.telemetry.nodestatus.NodeStatusProto.NodeStatus;
import static com.cloudera.thunderhead.telemetry.nodestatus.NodeStatusProto.NodeStatusReport;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.NODE_STATUS_MONITOR_FAILED;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.NODE_STATUS_MONITOR_FAILED_DETAILS;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.NODE_STATUS_MONITOR_UNREACHABLE;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.telemetry.nodestatus.NodeStatusProto;
import com.cloudera.thunderhead.telemetry.nodestatus.NodeStatusProto.HealthStatus;
import com.cloudera.thunderhead.telemetry.nodestatus.NodeStatusProto.ServiceStatus;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.client.RPCResponse;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.node.status.NodeStatusService;

@Component
public class NodeServicesCheckerConclusionStep extends ConclusionStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(NodeServicesCheckerConclusionStep.class);

    @Inject
    private NodeStatusService nodeStatusService;

    @Inject
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Override
    public Conclusion check(Long resourceId) {
        RPCResponse<NodeStatusReport> servicesReport;
        try {
            servicesReport = nodeStatusService.getServicesReport(resourceId);
            LOGGER.debug("Node services report response: {}", servicesReport == null ? null : servicesReport.getFirstTextMessage());
        } catch (Exception e) {
            LOGGER.warn("Node services report failed, error: {}", e.getMessage());
            return failed(cloudbreakMessagesService.getMessage(NODE_STATUS_MONITOR_UNREACHABLE), e.getMessage());
        }
        if (servicesReport != null && servicesReport.getResult() == null) {
            LOGGER.info("Node services report result was null");
            return succeeded();
        }

        Multimap<String, String> nodesWithUnhealthyServices = null;
        if (servicesReport != null) {
            nodesWithUnhealthyServices = collectNodesWithUnhealthyServices(servicesReport);
        }
        if (nodesWithUnhealthyServices == null || nodesWithUnhealthyServices.isEmpty()) {
            return succeeded();
        } else {
            String conclusion = cloudbreakMessagesService.getMessageWithArgs(NODE_STATUS_MONITOR_FAILED, nodesWithUnhealthyServices.keySet());
            String details = cloudbreakMessagesService.getMessageWithArgs(NODE_STATUS_MONITOR_FAILED_DETAILS, nodesWithUnhealthyServices);
            LOGGER.warn(details);
            return failed(conclusion, details);
        }
    }

    private Multimap<String, String> collectNodesWithUnhealthyServices(RPCResponse<NodeStatusReport> servicesReport) {
        Multimap<String, String> nodesWithUnhealthyServices = HashMultimap.create();
        for (NodeStatus nodeStatus : servicesReport.getResult().getNodesList()) {
            LOGGER.debug("Check node services report for host: {}", nodeStatus);
            NodeStatusProto.StatusDetails statusDetails = nodeStatus.getStatusDetails();
            if (statusDetails != null && HealthStatus.NOK.equals(statusDetails.getStatus())) {
                nodesWithUnhealthyServices.put(statusDetails.getHost(), "Unhealthy node: " + statusDetails.getStatusReason());
            }
            NodeStatusProto.ServicesDetails servicesDetails = nodeStatus.getServicesDetails();
            for (ServiceStatus serviceStatus : servicesDetails.getInfraServicesList()) {
                if (HealthStatus.NOK.equals(serviceStatus.getStatus())) {
                    nodesWithUnhealthyServices.put(statusDetails.getHost(), serviceStatus.getName());
                }
            }
            for (ServiceStatus serviceStatus : servicesDetails.getCmServicesList()) {
                if (HealthStatus.NOK.equals(serviceStatus.getStatus())) {
                    nodesWithUnhealthyServices.put(statusDetails.getHost(), serviceStatus.getName());
                }
            }
        }
        return nodesWithUnhealthyServices;
    }
}
