package com.sequenceiq.freeipa.service.freeipa.flow;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.flow.core.EventParameterFactory;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class FreeIpaEventParameterFactory implements EventParameterFactory {

    @Inject
    private StackService stackService;

    public Map<String, Object> createEventParameters(Long stackId) {
        String userCrn;
        try {
            userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        } catch (RuntimeException ex) {
            userCrn = stackService.getStackById(stackId).getOwner();
        }
        return Map.of(FlowConstants.FLOW_TRIGGER_USERCRN, userCrn);
    }
}
