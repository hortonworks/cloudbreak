package com.sequenceiq.cloudbreak.service.cluster.clouderamanager;

import java.math.BigDecimal;

import com.cloudera.api.swagger.client.ApiClient;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

public class ClouderaManagerTemplateInstallPollerObject extends ClouderaManagerPollerObject {

    private BigDecimal id;

    public ClouderaManagerTemplateInstallPollerObject(Stack stack, ApiClient apiClient, BigDecimal id) {
        super(stack, apiClient);
        this.id = id;
    }

    public BigDecimal getId() {
        return id;
    }
}
