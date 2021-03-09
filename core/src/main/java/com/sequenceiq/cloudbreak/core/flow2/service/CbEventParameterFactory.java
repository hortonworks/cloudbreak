package com.sequenceiq.cloudbreak.core.flow2.service;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.flow.core.EventParameterFactory;
import com.sequenceiq.flow.core.FlowConstants;

@Component
public class CbEventParameterFactory implements EventParameterFactory {

    @Override
    public Map<String, Object> createEventParameters(Long stackId) {
        String userCrn = getUserCrn();
        return Map.of(FlowConstants.FLOW_TRIGGER_USERCRN, userCrn);
    }

    private String getUserCrn() {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        if (userCrn == null) {
            throw new IllegalStateException("Cannot get user crn!");
        }
        return userCrn;
    }
}
