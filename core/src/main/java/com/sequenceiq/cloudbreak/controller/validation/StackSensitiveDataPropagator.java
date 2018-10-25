package com.sequenceiq.cloudbreak.controller.validation;

import javax.inject.Inject;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.api.model.CredentialSourceRequest;
import com.sequenceiq.cloudbreak.api.model.stack.StackRequest;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.decorator.CredentialSourceDecorator;

@Component
public class StackSensitiveDataPropagator {

    @Inject
    private CredentialSourceDecorator credentialSourceDecorator;

    public Stack propagate(StackRequest stackRequest, Stack stack, Workspace workspace) {
        CredentialSourceRequest credentialSourceRequest = stackRequest.getCredentialSource();
        if (StringUtils.isEmpty(stackRequest.getEnvironment()) && credentialSourceRequest != null) {
            Credential decorate = credentialSourceDecorator.decorate(stack.getCredential(), credentialSourceRequest, workspace);
            stack.setCredential(decorate);
        }
        return stack;
    }
}
