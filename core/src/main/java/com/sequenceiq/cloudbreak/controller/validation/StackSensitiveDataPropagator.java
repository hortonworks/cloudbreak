package com.sequenceiq.cloudbreak.controller.validation;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.CredentialSourceRequest;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.decorator.CredentialSourceDecorator;

@Component
public class StackSensitiveDataPropagator {

    @Inject
    private CredentialSourceDecorator credentialSourceDecorator;

    public Stack propagate(CredentialSourceRequest credentialSourceRequest, Stack stack, Workspace workspace) {
        if (credentialSourceRequest != null) {
            Credential decorate = credentialSourceDecorator.decorate(stack.getCredential(), credentialSourceRequest, workspace);
            stack.setCredential(decorate);
        }
        return stack;
    }
}
