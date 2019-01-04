package com.sequenceiq.cloudbreak.service.cluster.clouderamanager;

import com.cloudera.api.swagger.client.ApiClient;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.StackContext;

public class ClouderaManagerPollerObject extends StackContext {

    private ApiClient apiClient;

    public ClouderaManagerPollerObject(Stack stack, ApiClient apiClient) {
        super(stack);
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }
}
