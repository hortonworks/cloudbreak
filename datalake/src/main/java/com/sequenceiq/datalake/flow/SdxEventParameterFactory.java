package com.sequenceiq.datalake.flow;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.security.CrnUserDetailsService;
import com.sequenceiq.flow.core.EventParameterFactory;

@Component
public class SdxEventParameterFactory extends EventParameterFactory {

    public SdxEventParameterFactory(CrnUserDetailsService crnUserDetailsService) {
        super(crnUserDetailsService);
    }

    @Override
    protected Optional<String> getUserCrnByResourceId(Long resourceId) {
        return Optional.empty();
    }
}
