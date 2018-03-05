package com.sequenceiq.cloudbreak.blueprint;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.blueprint.nifi.HdfConfigs;
import com.sequenceiq.cloudbreak.blueprint.templates.BlueprintStackInfo;
import com.sequenceiq.cloudbreak.cloud.model.AmbariDatabase;
import com.sequenceiq.cloudbreak.common.model.OrchestratorType;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Gateway;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.Stack;

public class BlueprintPreparationObject {

    private final Stack stack;

    private final Cluster cluster;

    private final Gateway gateway;

    private final Set<RDSConfig> rdsConfigs;

    private final AmbariClient ambariClient;

    private final Set<HostGroup> hostGroups;

    private final Optional<String> stackRepoDetailsHdpVersion;

    private final String identityUserEmail;

    private final AmbariDatabase ambariDatabase;

    private final Optional<String> smartSenseSubscriptionId;

    private final OrchestratorType orchestratorType;

    private final Map<String, List<String>> fqdns;

    private final Optional<LdapConfig> ldapConfig;

    private final BlueprintStackInfo blueprintStackInfo;

    private final Optional<HdfConfigs> hdfConfigs;

    private BlueprintPreparationObject(BlueprintPreparationObject.Builder builder) {
        this.ambariClient = builder.ambariClient;
        this.stack = builder.stack;
        this.cluster = builder.cluster;
        this.rdsConfigs = builder.rdsConfigs;
        this.hostGroups = builder.hostGroups;
        this.stackRepoDetailsHdpVersion = builder.stackRepoDetailsHdpVersion;
        this.identityUserEmail = builder.identityUserEmail;
        this.ambariDatabase = builder.ambariDatabase;
        this.smartSenseSubscriptionId = builder.smartSenseSubscriptionId;
        this.orchestratorType = builder.orchestratorType;
        this.fqdns = builder.fqdns;
        this.ldapConfig = builder.ldapConfig;
        this.blueprintStackInfo = builder.blueprintStackInfo;
        this.hdfConfigs = builder.hdfConfigs;
        this.gateway = builder.gateway;
    }

    public Stack getStack() {
        return stack;
    }

    public Cluster getCluster() {
        return cluster;
    }

    public Set<RDSConfig> getRdsConfigs() {
        return rdsConfigs;
    }

    public AmbariClient getAmbariClient() {
        return ambariClient;
    }

    public Set<HostGroup> getHostGroups() {
        return hostGroups;
    }

    public Optional<String> getStackRepoDetailsHdpVersion() {
        return stackRepoDetailsHdpVersion;
    }

    public String getIdentityUserEmail() {
        return identityUserEmail;
    }

    public AmbariDatabase getAmbariDatabase() {
        return ambariDatabase;
    }

    public Optional<String> getSmartSenseSubscriptionId() {
        return smartSenseSubscriptionId;
    }

    public OrchestratorType getOrchestratorType() {
        return orchestratorType;
    }

    public InstanceMetaData getPrimaryGatewayInstance() {
        return stack.getPrimaryGatewayInstance();
    }

    public Set<InstanceGroup> getInstanceGroups() {
        return stack.getInstanceGroups();
    }

    public Map<String, List<String>> getFqdns() {
        return fqdns;
    }

    public Optional<LdapConfig> getLdapConfig() {
        return ldapConfig;
    }

    public BlueprintStackInfo getBlueprintStackInfo() {
        return blueprintStackInfo;
    }

    public Optional<HdfConfigs> getHdfConfigs() {
        return hdfConfigs;
    }

    public Gateway getGateway() {
        return gateway;
    }

    public static class Builder {

        private Stack stack;

        private Cluster cluster;

        private Set<RDSConfig> rdsConfigs = new HashSet<>();

        private AmbariClient ambariClient;

        private Set<HostGroup> hostGroups = new HashSet<>();

        private Optional<String> stackRepoDetailsHdpVersion = Optional.empty();

        private String identityUserEmail;

        private AmbariDatabase ambariDatabase;

        private Optional<String> smartSenseSubscriptionId = Optional.empty();

        private OrchestratorType orchestratorType = OrchestratorType.HOST;

        private Map<String, List<String>> fqdns = new HashMap<>();

        private Optional<LdapConfig> ldapConfig = Optional.empty();

        private BlueprintStackInfo blueprintStackInfo;

        private Optional<HdfConfigs> hdfConfigs = Optional.empty();

        private Gateway gateway;

        public static Builder builder() {
            return new Builder();
        }

        public Builder withAmbariDatabase(AmbariDatabase ambariDatabase) {
            this.ambariDatabase = ambariDatabase;
            return this;
        }

        public Builder withFqdns(Map<String, List<String>> fqdns) {
            this.fqdns = fqdns;
            return this;
        }

        public Builder withOrchestratorType(OrchestratorType orchestratorType) {
            this.orchestratorType = orchestratorType;
            return this;
        }

        public Builder withSmartSenseSubscriptionId(Optional<String> smartSenseSubscriptionId) {
            this.smartSenseSubscriptionId = smartSenseSubscriptionId;
            return this;
        }

        public Builder withStack(Stack stack) {
            this.stack = stack;
            return this;
        }

        public Builder withCluster(Cluster cluster) {
            this.cluster = cluster;
            return this;
        }

        public Builder withRdsConfigs(Set<RDSConfig> rdsConfigs) {
            this.rdsConfigs = rdsConfigs;
            return this;
        }

        public Builder withAmbariClient(AmbariClient ambariClient) {
            this.ambariClient = ambariClient;
            return this;
        }

        public Builder withHostgroups(Set<HostGroup> hostGroups) {
            this.hostGroups = hostGroups;
            return this;
        }

        public Builder withStackRepoDetailsHdpVersion(Optional<String> stackRepoDetailsHdpVersion) {
            this.stackRepoDetailsHdpVersion = stackRepoDetailsHdpVersion;
            return this;
        }

        public Builder withIdentityUserEmail(String identityUserEmail) {
            this.identityUserEmail = identityUserEmail;
            return this;
        }

        public Builder withLdapConfig(Optional<LdapConfig> ldapConfig) {
            this.ldapConfig = ldapConfig;
            return this;
        }

        public Builder withHdfConfigs(Optional<HdfConfigs> hdfConfigs) {
            this.hdfConfigs = hdfConfigs;
            return this;
        }

        public Builder withGateway(Gateway gateway) {
            this.gateway = gateway;
            return this;
        }

        public Builder withBlueprintStackInfo(BlueprintStackInfo blueprintStackInfo) {
            this.blueprintStackInfo = blueprintStackInfo;
            return this;
        }

        public BlueprintPreparationObject build() {
            return new BlueprintPreparationObject(this);
        }
    }
}
