package com.sequenceiq.cloudbreak.service.upgrade.ccm;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.CcmUpgradeEvent.CCM_UPGRADE_EVENT;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorNotifier;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class StackCcmUpgradeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackCcmUpgradeService.class);

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private StackService stackService;

    @Inject
    private ReactorNotifier reactorNotifier;

    public FlowIdentifier upgradeCcm(NameOrCrn nameOrCrn) {
        Long workspaceId = restRequestThreadLocalService.getRequestedWorkspaceId();
        Stack stack = stackService.getByNameOrCrnInWorkspace(nameOrCrn, workspaceId);
        MDCBuilder.buildMdcContext(stack);
        LOGGER.debug("CCM upgrade has been initiated for stack {}", nameOrCrn.getNameOrCrn());
        String selector = CCM_UPGRADE_EVENT.event();
        return reactorNotifier.notify(stack.getId(), selector, new StackEvent(selector, stack.getId()));
    }
}
