package com.sequenceiq.cloudbreak.cm.polling;

import com.cloudera.api.swagger.client.ApiClient;
import com.sequenceiq.cloudbreak.cluster.service.StackAware;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;

public class ClouderaManagerPollerObject implements StackAware {

    private final StackDtoDelegate stack;

    private final ApiClient apiClient;

    public ClouderaManagerPollerObject(StackDtoDelegate stack, ApiClient apiClient) {
        this.stack = stack;
        this.apiClient = apiClient;
    }

    @Override
    public StackDtoDelegate getStack() {
        return stack;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }
}
