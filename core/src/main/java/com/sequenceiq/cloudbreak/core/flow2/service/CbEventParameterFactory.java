package com.sequenceiq.cloudbreak.core.flow2.service;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.security.CrnUserDetailsService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.flow.core.EventParameterFactory;

@Component
public class CbEventParameterFactory extends EventParameterFactory {

    private final StackService stackService;

    public CbEventParameterFactory(CrnUserDetailsService crnUserDetailsService, StackService stackService) {
        super(crnUserDetailsService);
        this.stackService = stackService;
    }

    @Override
    protected Optional<String> getUserCrnByResourceId(Long resourceId) {
        return Optional.ofNullable(stackService.get(resourceId))
                .map(Stack::getCreator)
                .map(User::getUserCrn);
    }
}
