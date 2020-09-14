package com.sequenceiq.cloudbreak.cm.polling;

import com.cloudera.api.swagger.client.ApiClient;
import com.sequenceiq.cloudbreak.cluster.service.StackAware;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

public class ClouderaManagerPollerObject implements StackAware {

    private final Stack stack;

    private final ApiClient apiClient;

    public ClouderaManagerPollerObject(Stack stack, ApiClient apiClient) {
        this.stack = stack;
        this.apiClient = apiClient;
    }

    @Override
    public Stack getStack() {
        return stack;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }
}
