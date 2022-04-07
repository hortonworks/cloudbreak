package com.sequenceiq.consumption.flow;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.flow.core.EventParameterFactory;
import com.sequenceiq.flow.core.FlowConstants;

@Component
public class ConsumptionEventParameterFactory implements EventParameterFactory {

    public Map<String, Object> createEventParameters(Long id) {
        Optional<String> userCrn = Optional.empty();
        try {
            userCrn = Optional.of(ThreadBasedUserCrnProvider.getUserCrn());
        } catch (RuntimeException ex) {
        }
        return userCrn.isPresent() ? Map.of(FlowConstants.FLOW_TRIGGER_USERCRN, userCrn.get()) : Map.of();
    }
}
