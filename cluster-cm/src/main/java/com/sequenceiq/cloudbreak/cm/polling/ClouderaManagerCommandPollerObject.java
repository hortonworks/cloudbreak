package com.sequenceiq.cloudbreak.cm.polling;

import java.math.BigDecimal;

import com.cloudera.api.swagger.client.ApiClient;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

public class ClouderaManagerCommandPollerObject extends ClouderaManagerPollerObject {

    private final Stack stack;

    private final ApiClient apiClient;

    private final BigDecimal id;

    public ClouderaManagerCommandPollerObject(Stack stack, ApiClient apiClient, BigDecimal id) {
        super(stack, apiClient);
        this.stack = stack;
        this.apiClient = apiClient;
        this.id = id;
    }

    public BigDecimal getId() {
        return id;
    }
}
