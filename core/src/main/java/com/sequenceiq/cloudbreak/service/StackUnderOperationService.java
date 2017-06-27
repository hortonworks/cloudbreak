package com.sequenceiq.cloudbreak.service;

import org.springframework.stereotype.Service;

@Service
public class StackUnderOperationService {

    private ThreadLocal<Boolean> onLocal = new ThreadLocal<>();

    private ThreadLocal<Long> stackIdLocal = new ThreadLocal<>();

    public void on() {
        onLocal.set(Boolean.TRUE);
    }

    public void off() {
        stackIdLocal.remove();
        onLocal.remove();
    }

    public void set(Long stackId) {
        if (onLocal.get() == Boolean.TRUE) {
            stackIdLocal.set(stackId);
        } else {
            onLocal.remove();
        }
    }

    public Long get() {
        Long stackId = stackIdLocal.get();
        if (stackId == null) {
            stackIdLocal.remove();
        }
        return stackId;
    }
}
