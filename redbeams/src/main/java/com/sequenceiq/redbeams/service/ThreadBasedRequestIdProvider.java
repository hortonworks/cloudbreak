package com.sequenceiq.redbeams.service;

import javax.annotation.Nullable;

import org.springframework.stereotype.Service;

@Service
public class ThreadBasedRequestIdProvider {

    private static final ThreadLocal<String> REQUEST_ID = new InheritableThreadLocal<>();

    @Nullable
    public String getRequestId() {
        return REQUEST_ID.get();
    }

    public void setRequestId(String requestId) {
        REQUEST_ID.set(requestId);
    }

    public void removeRequestId() {
        REQUEST_ID.remove();
    }
}
