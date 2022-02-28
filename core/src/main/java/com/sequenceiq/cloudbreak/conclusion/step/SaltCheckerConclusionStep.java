package com.sequenceiq.cloudbreak.conclusion.step;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.telemetry.nodestatus.NodeStatusProto.HealthStatus;
import com.cloudera.thunderhead.telemetry.nodestatus.NodeStatusProto.SaltHealthReport;
import com.cloudera.thunderhead.telemetry.nodestatus.NodeStatusProto.SaltMasterHealth;
import com.cloudera.thunderhead.telemetry.nodestatus.NodeStatusProto.SaltMinionsHealth;
import com.cloudera.thunderhead.telemetry.nodestatus.NodeStatusProto.ServiceStatus;
import com.cloudera.thunderhead.telemetry.nodestatus.NodeStatusProto.StatusDetails;
import com.sequenceiq.cloudbreak.client.RPCResponse;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.node.status.NodeStatusService;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.NodesUnreachableException;
import com.sequenceiq.cloudbreak.util.StackUtil;

@Component
public class SaltCheckerConclusionStep extends ConclusionStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltCheckerConclusionStep.class);

    @Inject
    private NodeStatusService nodeStatusService;

    @Inject
    private StackService stackService;

    @Inject
    private StackUtil stackUtil;

    @Override
    public Conclusion check(Long resourceId) {
        RPCResponse<SaltHealthReport> saltPingResponse;
        try {
            saltPingResponse = nodeStatusService.saltPing(resourceId);
        } catch (CloudbreakServiceException e) {
            LOGGER.debug("Salt health report failed, fallback and check unreachable nodes, error: {}", e.getMessage());
            return checkUnreachableNodes(resourceId);
        }
        SaltHealthReport report = saltPingResponse.getResult();
        if (report == null) {
            LOGGER.debug("Salt health report was null, fallback and check unreachable nodes.");
            return checkUnreachableNodes(resourceId);
        }

        List<String> failedServicesOnMaster = collectFailedServicesOnMaster(report);
        if (!failedServicesOnMaster.isEmpty()) {
            return createFailedConclusionForMaster(failedServicesOnMaster);
        } else {
            Map<String, String> unreachableMinions = collectUnreachableMinions(report);
            if (unreachableMinions.isEmpty()) {
                return succeeded();
            } else {
                return createFailedConclusionForMinions(unreachableMinions);
            }
        }
    }

    private Conclusion checkUnreachableNodes(Long resourceId) {
        Stack stack = stackService.getByIdWithListsInTransaction(resourceId);
        Set<String> allNodes = stackUtil.collectNodes(stack).stream().map(Node::getHostname).collect(Collectors.toSet());
        try {
            stackUtil.collectAndCheckReachableNodes(stack, allNodes);
        } catch (NodesUnreachableException e) {
            Set<String> unreachableNodes = e.getUnreachableNodes();
            String conclusion = String.format("Unreachable nodes: %s. We detected that cluster members can’t communicate with each other. " +
                            "Please validate if all cluster members are available and healthy through your cloud provider.", unreachableNodes);
            String details = String.format("Unreachable salt minions: %s", unreachableNodes);
            LOGGER.warn(details);
            return failed(conclusion, details);
        }
        return succeeded();
    }

    private List<String> collectFailedServicesOnMaster(SaltHealthReport report) {
        SaltMasterHealth saltMasterHealth = report.getMaster();
        return saltMasterHealth.getServicesList().stream()
                .filter(serviceStatus -> HealthStatus.NOK.equals(serviceStatus.getStatus()))
                .map(ServiceStatus::getName)
                .collect(Collectors.toList());
    }

    private Map<String, String> collectUnreachableMinions(SaltHealthReport report) {
        SaltMinionsHealth saltMinionsHealth = report.getMinions();
        return saltMinionsHealth.getPingResponsesList().stream()
                .filter(statusDetails -> HealthStatus.NOK.equals(statusDetails.getStatus()))
                .collect(Collectors.toMap(StatusDetails::getHost, StatusDetails::getStatusReason));
    }

    private Conclusion createFailedConclusionForMaster(List<String> failedServicesOnMaster) {
        String conclusion = String.format("There are unhealthy services on master node: %s. " +
                        "Please check the instances on your cloud provider for further details.", failedServicesOnMaster);
        String details = String.format("Unhealthy services on master: %s", StringUtils.join(failedServicesOnMaster));
        LOGGER.warn(details);
        return failed(conclusion, details);
    }

    private Conclusion createFailedConclusionForMinions(Map<String, String> unreachableMinions) {
        String conclusion = String.format("Unreachable nodes: %s. We detected that cluster members can’t communicate with each other. " +
                        "Please validate if all cluster members are available and healthy through your cloud provider.", unreachableMinions.keySet());
        String details = String.format("Unreachable salt minions: %s", StringUtils.join(unreachableMinions));
        LOGGER.warn(details);
        return failed(conclusion, details);
    }
}
