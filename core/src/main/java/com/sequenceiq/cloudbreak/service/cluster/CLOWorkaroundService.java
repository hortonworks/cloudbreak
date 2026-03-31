package com.sequenceiq.cloudbreak.service.cluster;

import java.util.List;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateService;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.event.DistroXUpgradeFlowChainTriggerEvent;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.SaltUpdateTriggerEvent;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class CLOWorkaroundService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CLOWorkaroundService.class);

    private static final String CLO_SERVICE_TYPE = "LAKEHOUSE_OPTIMIZER";

    @Inject
    private CmTemplateService cmTemplateService;

    @Inject
    private StackService stackService;

    public List<Selectable> getClusterUpgradeTriggerEvent(DistroXUpgradeFlowChainTriggerEvent event) {
        Stack stack = stackService.get(event.getResourceId());
        if (cloPresentedOnTheCluster(stack) && targetVersionIs732(event)) {
            LOGGER.info("CLO workaround for modifing the knox topology will be applied");
            return List.of(new SaltUpdateTriggerEvent(event.getResourceId(), false));
        }
        return List.of();
    }

    private boolean cloPresentedOnTheCluster(Stack stack) {
        return cmTemplateService.isServiceTypePresent(CLO_SERVICE_TYPE, stack.getBlueprintJsonText());
    }

    private boolean targetVersionIs732(DistroXUpgradeFlowChainTriggerEvent event) {
        return StringUtils.isNotBlank(event.getRuntimeVersion())
                && event.getRuntimeVersion().equalsIgnoreCase("7.3.2");
    }
}