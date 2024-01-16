package com.sequenceiq.freeipa.service.freeipa.flow;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.security.CrnUserDetailsService;
import com.sequenceiq.flow.core.EventParameterFactory;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class FreeIpaEventParameterFactory extends EventParameterFactory {

    private final StackService stackService;

    public FreeIpaEventParameterFactory(CrnUserDetailsService crnUserDetailsService, StackService stackService) {
        super(crnUserDetailsService);
        this.stackService = stackService;
    }

    @Override
    protected Optional<String> getUserCrnByResourceId(Long resourceId) {
        return Optional.ofNullable(stackService.getStackById(resourceId))
                .map(Stack::getOwner);
    }
}
