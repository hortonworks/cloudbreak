package com.sequenceiq.cloudbreak.conclusion.step;

import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.SALT_COLLECT_UNREACHABLE_FAILED;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.SALT_COLLECT_UNREACHABLE_FAILED_DETAILS;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.SALT_COLLECT_UNREACHABLE_FOUND;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.SALT_COLLECT_UNREACHABLE_FOUND_DETAILS;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.SALT_MASTER_SERVICES_UNHEALTHY;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.SALT_MASTER_SERVICES_UNHEALTHY_DETAILS;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.SALT_MINIONS_UNREACHABLE;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.SALT_MINIONS_UNREACHABLE_DETAILS;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

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
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.node.status.NodeStatusService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.util.NodesUnreachableException;
import com.sequenceiq.cloudbreak.util.StackUtil;

@Component
public class SaltCheckerConclusionStep extends ConclusionStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltCheckerConclusionStep.class);

    @Inject
    private NodeStatusService nodeStatusService;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Override
    public Conclusion check(Long resourceId) {
        RPCResponse<SaltHealthReport> saltPingResponse;
        try {
            saltPingResponse = nodeStatusService.saltPing(resourceId);
            LOGGER.debug("Salt health report response: {}", saltPingResponse.getFirstTextMessage());
        } catch (Exception e) {
            LOGGER.debug("Salt health report failed, fallback and check unreachable nodes, error: {}", e.getMessage());
            return checkUnreachableNodes(resourceId);
        }
        SaltHealthReport report = saltPingResponse.getResult();
        if (report == null) {
            LOGGER.info("Salt health report was null, fallback and check unreachable nodes.");
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
        StackDto stackDto = stackDtoService.getById(resourceId);
        Set<String> allAvailableInstanceHostNames = stackDto.getAllAvailableInstances().stream().map(i -> i.getDiscoveryFQDN()).collect(Collectors.toSet());
        Set<String> availableNodes = stackUtil.collectNodes(stackDto, allAvailableInstanceHostNames)
                .stream().map(Node::getHostname).collect(Collectors.toSet());
        try {
            stackUtil.collectReachableAndCheckNecessaryNodes(stackDto, availableNodes);
        } catch (NodesUnreachableException e) {
            Set<String> unreachableNodes = e.getUnreachableNodes();
            String conclusion = cloudbreakMessagesService.getMessageWithArgs(SALT_COLLECT_UNREACHABLE_FOUND, unreachableNodes);
            String details = cloudbreakMessagesService.getMessageWithArgs(SALT_COLLECT_UNREACHABLE_FOUND_DETAILS, unreachableNodes);
            LOGGER.warn(details);
            return failed(conclusion, details);
        } catch (Exception e) {
            String conclusion = cloudbreakMessagesService.getMessage(SALT_COLLECT_UNREACHABLE_FAILED);
            String details = cloudbreakMessagesService.getMessageWithArgs(SALT_COLLECT_UNREACHABLE_FAILED_DETAILS, e.getMessage());
            LOGGER.warn(details, e);
            return failed(conclusion, details);
        }
        LOGGER.debug("All available nodes are reachable based on salt ping: {}", availableNodes);
        return succeeded();
    }

    private List<String> collectFailedServicesOnMaster(SaltHealthReport report) {
        SaltMasterHealth saltMasterHealth = report.getMaster();
        LOGGER.debug("Salt master health report: {}", saltMasterHealth);
        return saltMasterHealth.getServicesList().stream()
                .filter(serviceStatus -> HealthStatus.NOK.equals(serviceStatus.getStatus()))
                .map(ServiceStatus::getName)
                .collect(Collectors.toList());
    }

    private Map<String, String> collectUnreachableMinions(SaltHealthReport report) {
        SaltMinionsHealth saltMinionsHealth = report.getMinions();
        LOGGER.debug("Salt minions health report: {}", saltMinionsHealth);
        return saltMinionsHealth.getPingResponsesList().stream()
                .filter(statusDetails -> HealthStatus.NOK.equals(statusDetails.getStatus()))
                .collect(Collectors.toMap(StatusDetails::getHost, StatusDetails::getStatusReason));
    }

    private Conclusion createFailedConclusionForMaster(List<String> failedServicesOnMaster) {
        String conclusion = cloudbreakMessagesService.getMessageWithArgs(SALT_MASTER_SERVICES_UNHEALTHY, failedServicesOnMaster);
        String details = cloudbreakMessagesService.getMessageWithArgs(SALT_MASTER_SERVICES_UNHEALTHY_DETAILS, failedServicesOnMaster);
        LOGGER.warn(details);
        return failed(conclusion, details);
    }

    private Conclusion createFailedConclusionForMinions(Map<String, String> unreachableMinions) {
        String conclusion = cloudbreakMessagesService.getMessageWithArgs(SALT_MINIONS_UNREACHABLE, unreachableMinions.keySet());
        String details = cloudbreakMessagesService.getMessageWithArgs(SALT_MINIONS_UNREACHABLE_DETAILS, unreachableMinions);
        LOGGER.warn(details);
        return failed(conclusion, details);
    }
}
