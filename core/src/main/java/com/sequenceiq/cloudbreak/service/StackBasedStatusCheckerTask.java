package com.sequenceiq.cloudbreak.service;

import javax.inject.Inject;

import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackRepository;

public abstract class StackBasedStatusCheckerTask<T extends StackContext> extends SimpleStatusCheckerTask<T> {

    @Inject
    private StackRepository stackRepository;

    public boolean exitPolling(T t) {
        try {
            Stack stack = stackRepository.findByIdLazy(t.getStack().getId());
            if (stack == null || stack.isDeleteInProgress() || stack.isDeleteCompleted()) {
                return true;
            }
            return false;
        } catch (Exception ex) {
            return true;
        }
    }

}
