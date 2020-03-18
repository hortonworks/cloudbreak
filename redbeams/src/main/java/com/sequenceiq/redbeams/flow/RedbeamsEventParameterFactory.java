package com.sequenceiq.redbeams.flow;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.flow.core.EventParameterFactory;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.redbeams.service.stack.DBStackService;

@Component
public class RedbeamsEventParameterFactory implements EventParameterFactory {

    @Inject
    private DBStackService stackService;

    public Map<String, Object> createEventParameters(Long stackId) {
        String userCrn;
        try {
            userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        } catch (RuntimeException ex) {
            userCrn = stackService.getById(stackId).getOwnerCrn().toString();
        }
        return Map.of(FlowConstants.FLOW_TRIGGER_USERCRN, userCrn);
    }
}
