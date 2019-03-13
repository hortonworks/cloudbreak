package com.sequenceiq.cloudbreak.cm.polling;

import com.cloudera.api.swagger.client.ApiClient;
import com.sequenceiq.cloudbreak.cluster.service.StackAware;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

public class ClouderaManagerPollerObject implements StackAware {

    private ApiClient apiClient;

    private final Stack stack;

    public ClouderaManagerPollerObject(Stack stack, ApiClient apiClient) {
        this.stack = stack;
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    @Override
    public Stack getStack() {
        return stack;
    }
}
