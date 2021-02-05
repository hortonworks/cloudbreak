package com.sequenceiq.cloudbreak.service;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class LoadBalancerUpdateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoadBalancerUpdateService.class);

    @Inject
    private StackService stackService;

    @Inject
    private ReactorFlowManager flowManager;

    public FlowIdentifier updateLoadBalancers(NameOrCrn nameOrCrn, Long workspaceId) {
        Stack stack = stackService.getByNameOrCrnInWorkspace(nameOrCrn, workspaceId);
        MDCBuilder.buildMdcContext(stack);
        LOGGER.info("Initiating load balancer update for stack {}", stack.getId());
        return flowManager.triggerStackLoadBalancerUpdate(stack.getId());
    }
}
