package com.sequenceiq.cloudbreak.cm.polling;

import com.cloudera.api.swagger.client.ApiClient;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

public class ClouderaManagerCommandPollerObject extends ClouderaManagerPollerObject {

    private final Stack stack;

    private final ApiClient apiClient;

    private final Integer id;

    public ClouderaManagerCommandPollerObject(Stack stack, ApiClient apiClient, Integer id) {
        super(stack, apiClient);
        this.stack = stack;
        this.apiClient = apiClient;
        this.id = id;
    }

    public Integer getId() {
        return id;
    }
}
