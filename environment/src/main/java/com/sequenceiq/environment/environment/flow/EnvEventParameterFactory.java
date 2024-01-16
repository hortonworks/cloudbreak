package com.sequenceiq.environment.environment.flow;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.security.CrnUserDetailsService;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.flow.core.EventParameterFactory;

@Component
public class EnvEventParameterFactory extends EventParameterFactory {

    private final EnvironmentService environmentService;

    public EnvEventParameterFactory(CrnUserDetailsService crnUserDetailsService, EnvironmentService environmentService) {
        super(crnUserDetailsService);
        this.environmentService = environmentService;
    }

    @Override
    protected Optional<String> getUserCrnByResourceId(Long resourceId) {
        return environmentService.findEnvironmentById(resourceId)
                .map(Environment::getCreator);
    }
}
