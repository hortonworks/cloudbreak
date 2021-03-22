package com.sequenceiq.cloudbreak.conclusion.step;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.NodesUnreachableException;
import com.sequenceiq.cloudbreak.util.StackUtil;

@Component
public class SaltMinionCheckerConclusionStep implements ConclusionStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltMinionCheckerConclusionStep.class);

    @Inject
    private StackService stackService;

    @Inject
    private StackUtil stackUtil;

    @Override
    public ConclusionStepResult check(Long resourceId) {
        Stack stack = stackService.getByIdWithListsInTransaction(resourceId);
        Set<String> allNodes = stackUtil.collectNodes(stack).stream().map(Node::getHostname).collect(Collectors.toSet());
        try {
            stackUtil.collectAndCheckReachableNodes(stack, allNodes);
        } catch (NodesUnreachableException e) {
            Set<String> unreachableNodes = e.getUnreachableNodes();
            LOGGER.error("Unreachable salt minions: {}", unreachableNodes);
            String conclusion = String.format("Unreachable nodes: %s. Please check the instances on your cloud provider for further details.",
                    unreachableNodes);
            return ConclusionStepResult.failed(conclusion);
        }
        return ConclusionStepResult.succeeded();
    }
}
