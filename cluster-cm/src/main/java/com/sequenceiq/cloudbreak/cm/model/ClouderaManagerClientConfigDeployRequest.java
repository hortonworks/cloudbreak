package com.sequenceiq.cloudbreak.cm.model;

import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;

public record ClouderaManagerClientConfigDeployRequest(ClustersResourceApi api, ApiClient client, StackDtoDelegate stack, String message) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private ClustersResourceApi clustersResourceApi;

        private ApiClient client;

        private StackDtoDelegate stack;

        private String pollerMessage;

        public Builder clustersResourceApi(ClustersResourceApi clustersResourceApi) {
            this.clustersResourceApi = clustersResourceApi;
            return this;
        }

        public Builder client(ApiClient client) {
            this.client = client;
            return this;
        }

        public Builder stack(StackDtoDelegate stack) {
            this.stack = stack;
            return this;
        }

        public Builder pollerMessage(String pollerMessage) {
            this.pollerMessage = pollerMessage;
            return this;
        }

        public ClouderaManagerClientConfigDeployRequest build() {
            return new ClouderaManagerClientConfigDeployRequest(clustersResourceApi, client, stack, pollerMessage);
        }
    }
}

