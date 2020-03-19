package com.sequenceiq.environment.environment.flow;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.flow.core.EventParameterFactory;
import com.sequenceiq.flow.core.FlowConstants;

@Component
public class EnvEventParameterFactory implements EventParameterFactory {

    @Inject
    private EnvironmentService environmentService;

    public Map<String, Object> createEventParameters(Long id) {
        Optional<String> userCrn = Optional.empty();
        try {
            userCrn = Optional.of(ThreadBasedUserCrnProvider.getUserCrn());
        } catch (RuntimeException ex) {
            Optional<Environment> environment = environmentService.findEnvironmentById(id);
            if (environment.isPresent()) {
                userCrn = Optional.of(environment.get().getCreator());
            }
        }
        return userCrn.isPresent() ? Map.of(FlowConstants.FLOW_TRIGGER_USERCRN, userCrn.get()) : Map.of();
    }
}
