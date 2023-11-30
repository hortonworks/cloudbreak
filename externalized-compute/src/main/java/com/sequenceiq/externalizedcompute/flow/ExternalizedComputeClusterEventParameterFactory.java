package com.sequenceiq.externalizedcompute.flow;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.security.CrnUserDetailsService;
import com.sequenceiq.flow.core.EventParameterFactory;

@Component
public class ExternalizedComputeClusterEventParameterFactory extends EventParameterFactory {

    public ExternalizedComputeClusterEventParameterFactory(CrnUserDetailsService crnUserDetailsService) {
        super(crnUserDetailsService);
    }

    @Override
    protected Optional<String> getUserCrnByResourceId(Long resourceId) {
        return Optional.empty();
    }

}