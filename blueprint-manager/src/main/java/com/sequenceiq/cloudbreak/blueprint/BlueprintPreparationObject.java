package com.sequenceiq.cloudbreak.blueprint;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.cloud.model.AmbariDatabase;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.common.model.OrchestratorType;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.Stack;

public class BlueprintPreparationObject {

    private Stack stack;

    private Cluster cluster;

    private Set<RDSConfig> rdsConfigs;

    private AmbariClient ambariClient;

    private Set<HostGroup> hostGroups;

    private StackRepoDetails stackRepoDetails;

    private IdentityUser identityUser;

    private AmbariDatabase ambariDatabase;

    private Optional<String> smartSenseSubscriptionId;

    private OrchestratorType orchestratorType;

    private Map<String, List<String>> fqdns;

    private BlueprintPreparationObject(BlueprintPreparationObject.Builder builder) {
        this.ambariClient = builder.ambariClient;
        this.stack = builder.stack;
        this.cluster = builder.cluster;
        this.rdsConfigs = builder.rdsConfigs;
        this.hostGroups = builder.hostGroups;
        this.stackRepoDetails = builder.stackRepoDetails;
        this.identityUser = builder.identityUser;
        this.ambariDatabase = builder.ambariDatabase;
        this.smartSenseSubscriptionId = builder.smartSenseSubscriptionId;
        this.orchestratorType = builder.orchestratorType;
        this.fqdns = builder.fqdns;
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

    public StackRepoDetails getStackRepoDetails() {
        return stackRepoDetails;
    }

    public IdentityUser getIdentityUser() {
        return identityUser;
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

    public static class Builder {

        private Stack stack;

        private Cluster cluster;

        private Set<RDSConfig> rdsConfigs = new HashSet<>();

        private AmbariClient ambariClient;

        private Set<HostGroup> hostGroups = new HashSet<>();

        private StackRepoDetails stackRepoDetails;

        private IdentityUser identityUser;

        private AmbariDatabase ambariDatabase;

        private Optional<String> smartSenseSubscriptionId = Optional.empty();

        private OrchestratorType orchestratorType = OrchestratorType.HOST;

        private Map<String, List<String>> fqdns = new HashMap<>();

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

        public Builder withStackRepoDetails(StackRepoDetails stackRepoDetails) {
            this.stackRepoDetails = stackRepoDetails;
            return this;
        }

        public Builder withIdentityUser(IdentityUser identityUser) {
            this.identityUser = identityUser;
            return this;
        }

        public BlueprintPreparationObject build() {
            return new BlueprintPreparationObject(this);
        }
    }
}
