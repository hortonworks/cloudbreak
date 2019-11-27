package com.sequenceiq.cloudbreak.service;

import org.springframework.stereotype.Service;

@Service
public class StackUnderOperationService {

    private static final ThreadLocal<Boolean> ON_LOCAL = new ThreadLocal<>();

    private static final ThreadLocal<Long> STACK_ID_LOCAL = new ThreadLocal<>();

    public void on() {
        ON_LOCAL.set(Boolean.TRUE);
    }

    public void off() {
        STACK_ID_LOCAL.remove();
        ON_LOCAL.remove();
    }

    public void set(Long stackId) {
        if (Boolean.TRUE.equals(ON_LOCAL.get())) {
            STACK_ID_LOCAL.set(stackId);
        } else {
            ON_LOCAL.remove();
        }
    }

    public Long get() {
        Long stackId = STACK_ID_LOCAL.get();
        if (stackId == null) {
            STACK_ID_LOCAL.remove();
        }
        return stackId;
    }
}
