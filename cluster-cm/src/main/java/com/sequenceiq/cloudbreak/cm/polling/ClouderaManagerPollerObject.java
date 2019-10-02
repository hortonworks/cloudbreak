package com.sequenceiq.cloudbreak.cm.polling;

import java.math.BigDecimal;

import com.cloudera.api.swagger.client.ApiClient;
import com.sequenceiq.cloudbreak.cluster.service.StackAware;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

public class ClouderaManagerPollerObject implements StackAware {

    private final Stack stack;

    private final ApiClient apiClient;

    private final BigDecimal id;

    public ClouderaManagerPollerObject(Stack stack, ApiClient apiClient, BigDecimal id) {
        this.stack = stack;
        this.apiClient = apiClient;
        this.id = id;
    }

    @Override
    public Stack getStack() {
        return stack;
    }

    public BigDecimal getId() {
        return id;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }
}
