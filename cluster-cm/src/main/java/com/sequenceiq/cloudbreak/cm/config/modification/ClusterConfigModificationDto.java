package com.sequenceiq.cloudbreak.cm.config.modification;

import java.util.List;
import java.util.Map;

import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.model.ApiRoleConfigGroupList;
import com.cloudera.api.swagger.model.ApiServiceConfig;
import com.sequenceiq.cloudbreak.cluster.model.CMConfigUpdateStrategy;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;

public class ClusterConfigModificationDto {
    private final StackDtoDelegate stack;

    private final CmConfig newConfig;

    private final ApiClient client;

    private final List<CmServiceMetadata> cmServiceMetadata;

    private final Map<String, ApiServiceConfig> serviceConfigCache;

    private final Map<String, ApiRoleConfigGroupList> roleConfigGroupCache;

    private final CMConfigUpdateStrategy cmConfigUpdateStrategy;

    private ClusterConfigModificationDto(ClusterConfigModificationDto.Builder builder) {
        this.stack = builder.stack;
        this.newConfig = builder.newConfig;
        this.cmServiceMetadata = builder.cmServiceMetadata;
        this.serviceConfigCache = builder.serviceConfigCache;
        this.roleConfigGroupCache = builder.roleConfigGroupCache;
        this.client = builder.client;
        this.cmConfigUpdateStrategy = builder.cmConfigUpdateStrategy;
    }

    public StackDtoDelegate getStack() {
        return stack;
    }

    public CmConfig getNewConfig() {
        return newConfig;
    }

    public Map<String, ApiServiceConfig> getServiceConfigCache() {
        return serviceConfigCache;
    }

    public List<CmServiceMetadata> getCmServiceMetadata() {
        return cmServiceMetadata;
    }

    public Map<String, ApiRoleConfigGroupList> getRoleConfigGroupCache() {
        return roleConfigGroupCache;
    }

    public CMConfigUpdateStrategy getCmConfigUpdateStrategy() {
        return cmConfigUpdateStrategy;
    }

    public ApiClient getClient() {
        return client;
    }

    public static ClusterConfigModificationDto.Builder builder() {
        return new ClusterConfigModificationDto.Builder();
    }

    public static class Builder {
        private StackDtoDelegate stack;

        private CmConfig newConfig;

        private List<CmServiceMetadata> cmServiceMetadata;

        private Map<String, ApiServiceConfig> serviceConfigCache;

        private Map<String, ApiRoleConfigGroupList> roleConfigGroupCache;

        private ApiClient client;

        private CMConfigUpdateStrategy cmConfigUpdateStrategy;

        public ClusterConfigModificationDto.Builder withStack(StackDtoDelegate stack) {
            this.stack = stack;
            return this;
        }

        public ClusterConfigModificationDto.Builder withCmConfigUpdateStrategy(CMConfigUpdateStrategy cmConfigUpdateStrategy) {
            this.cmConfigUpdateStrategy = cmConfigUpdateStrategy;
            return this;
        }

        public ClusterConfigModificationDto.Builder withClient(ApiClient client) {
            this.client = client;
            return this;
        }

        public ClusterConfigModificationDto.Builder withNewConfig(CmConfig newConfig) {
            this.newConfig = newConfig;
            return this;
        }

        public ClusterConfigModificationDto.Builder withCmServiceMetadata(List<CmServiceMetadata> cmServiceMetadata) {
            this.cmServiceMetadata = cmServiceMetadata;
            return this;
        }

        public ClusterConfigModificationDto.Builder withServiceConfigCache(Map<String, ApiServiceConfig> serviceConfigCache) {
            this.serviceConfigCache = serviceConfigCache;
            return this;
        }

        public ClusterConfigModificationDto.Builder withRoleConfigGroupCache(Map<String, ApiRoleConfigGroupList> roleConfigGroupCache) {
            this.roleConfigGroupCache = roleConfigGroupCache;
            return this;
        }

        public ClusterConfigModificationDto build() {
            return new ClusterConfigModificationDto(this);
        }
    }
}