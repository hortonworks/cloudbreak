package com.sequenceiq.cloudbreak.core.flow2.service;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.security.authentication.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.flow.core.FlowConstants;

@Component
public class EventParameterFactory {

    @Inject
    private StackService stackService;

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    public Map<String, Object> createEventParameters(Long stackId) {
        String userCrn = getUserCrn(stackId);
        return Map.of(FlowConstants.FLOW_TRIGGER_USERCRN, userCrn);
    }

    private String getUserCrn(Long stackId) {
        String userCrn = null;
        try {
            userCrn = authenticatedUserService.getUserCrn();
        } catch (RuntimeException ex) {
            userCrn = stackService.findById(stackId).map(Stack::getCreator).map(User::getUserCrn)
                    .orElseThrow(() -> new IllegalStateException("No authentication found neither in the SecurityContextHolder nor in the Stack!"));
        }
        return userCrn;
    }
}
