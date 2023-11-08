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

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.salt.SaltOrchestrator;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.util.NodesUnreachableException;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@Component
public class SaltCheckerConclusionStep extends ConclusionStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltCheckerConclusionStep.class);

    @Inject
    private SaltOrchestrator saltOrchestrator;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Override
    public Conclusion check(Long resourceId) {
        StackDto stackDto = stackDtoService.getById(resourceId);
        try {
            GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stackDto);
            if (!saltOrchestrator.isBootstrapApiAvailable(primaryGatewayConfig)) {
                return createFailedConclusionForMaster(List.of("salt-bootstrap"));
            }
            Map<String, Boolean> pingResponses = saltOrchestrator.ping(primaryGatewayConfig);

            if (pingResponses.values().stream().allMatch(Boolean.TRUE::equals)) {
                return succeeded();
            } else {
                return createFailedConclusionForMinions(pingResponses.entrySet().stream()
                        .filter(entry -> !Boolean.TRUE.equals(entry.getValue()))
                        .map(Map.Entry::getKey)
                        .toList());
            }
        } catch (Exception e) {
            LOGGER.debug("Checking salt failed, fallback and check unreachable nodes, error: {}", e.getMessage());
            return checkUnreachableNodes(stackDto);
        }
    }

    private Conclusion checkUnreachableNodes(StackDto stackDto) {
        Set<String> allAvailableInstanceHostNames = stackDto.getAllAvailableInstances().stream()
                .map(InstanceMetadataView::getDiscoveryFQDN)
                .collect(Collectors.toSet());
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

    private Conclusion createFailedConclusionForMaster(List<String> failedServicesOnMaster) {
        String conclusion = cloudbreakMessagesService.getMessageWithArgs(SALT_MASTER_SERVICES_UNHEALTHY, failedServicesOnMaster);
        String details = cloudbreakMessagesService.getMessageWithArgs(SALT_MASTER_SERVICES_UNHEALTHY_DETAILS, failedServicesOnMaster);
        LOGGER.warn(details);
        return failed(conclusion, details);
    }

    private Conclusion createFailedConclusionForMinions(List<String> unreachableMinions) {
        String conclusion = cloudbreakMessagesService.getMessageWithArgs(SALT_MINIONS_UNREACHABLE, unreachableMinions);
        String details = cloudbreakMessagesService.getMessageWithArgs(SALT_MINIONS_UNREACHABLE_DETAILS, unreachableMinions);
        LOGGER.warn(details);
        return failed(conclusion, details);
    }
}
