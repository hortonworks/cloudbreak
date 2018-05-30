package com.sequenceiq.cloudbreak.blueprint;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.sequenceiq.cloudbreak.blueprint.filesystem.BaseFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.blueprint.nifi.HdfConfigs;
import com.sequenceiq.cloudbreak.blueprint.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.blueprint.template.views.GatewayView;
import com.sequenceiq.cloudbreak.blueprint.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.blueprint.template.views.SharedServiceConfigsView;
import com.sequenceiq.cloudbreak.blueprint.templates.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.domain.FlexSubscription;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;

public class BlueprintPreparationObject {

    private final GatewayView gatewayView;

    private final GeneralClusterConfigs generalClusterConfigs;

    private final BlueprintView blueprintView;

    private final Set<RDSConfig> rdsConfigs;

    private final Set<HostgroupView> hostgroupViews;

    private final Optional<String> smartSenseSubscriptionId;

    private final Optional<LdapConfig> ldapConfig;

    private final Optional<SharedServiceConfigsView> sharedServiceConfigs;

    private final Optional<String> stackRepoDetailsHdpVersion;

    private final Optional<HdfConfigs> hdfConfigs;

    private final Optional<BaseFileSystemConfigurationsView> fileSystemView;

    private final Optional<KerberosConfig> kerberosConfig;

    private final Optional<FlexSubscription> flexSubscription;

    private final Map<String, Object> customInputs;

    private final Map<String, Object> fixInputs;

    private BlueprintPreparationObject(BlueprintPreparationObject.Builder builder) {
        this.rdsConfigs = builder.rdsConfigs;
        this.hostgroupViews = builder.hostgroupViews;
        this.stackRepoDetailsHdpVersion = builder.stackRepoDetailsHdpVersion;
        this.smartSenseSubscriptionId = builder.smartSenseSubscriptionId;
        this.ldapConfig = builder.ldapConfig;
        this.hdfConfigs = builder.hdfConfigs;
        this.gatewayView = builder.gatewayView;
        this.fileSystemView = builder.fileSystemView;
        this.kerberosConfig = builder.kerberosConfig;
        this.blueprintView = builder.blueprintView;
        this.generalClusterConfigs = builder.generalClusterConfigs;
        this.flexSubscription = builder.flexSubscription;
        this.sharedServiceConfigs = builder.sharedServiceConfigs;
        this.customInputs = builder.customInputs;
        this.fixInputs = builder.fixInputs;
    }

    public Set<RDSConfig> getRdsConfigs() {
        return rdsConfigs;
    }

    public Set<HostgroupView> getHostgroupViews() {
        return hostgroupViews;
    }

    public Optional<String> getStackRepoDetailsHdpVersion() {
        return stackRepoDetailsHdpVersion;
    }

    public Optional<String> getSmartSenseSubscriptionId() {
        return smartSenseSubscriptionId;
    }

    public Optional<LdapConfig> getLdapConfig() {
        return ldapConfig;
    }

    public Optional<HdfConfigs> getHdfConfigs() {
        return hdfConfigs;
    }

    public GatewayView getGatewayView() {
        return gatewayView;
    }

    public Optional<KerberosConfig> getKerberosConfig() {
        return kerberosConfig;
    }

    public Optional<BaseFileSystemConfigurationsView> getFileSystemConfigurationView() {
        return fileSystemView;
    }

    public GeneralClusterConfigs getGeneralClusterConfigs() {
        return generalClusterConfigs;
    }

    public Optional<FlexSubscription> getFlexSubscription() {
        return flexSubscription;
    }

    public BlueprintView getBlueprintView() {
        return blueprintView;
    }

    public Optional<SharedServiceConfigsView> getSharedServiceConfigs() {
        return sharedServiceConfigs;
    }

    public Map<String, Object> getCustomInputs() {
        return customInputs;
    }

    public Map<String, Object> getFixInputs() {
        return fixInputs;
    }

    public static class Builder {

        private Set<RDSConfig> rdsConfigs = new HashSet<>();

        private Set<HostgroupView> hostgroupViews = new HashSet<>();

        private Optional<String> stackRepoDetailsHdpVersion = Optional.empty();

        private Optional<String> smartSenseSubscriptionId = Optional.empty();

        private Optional<LdapConfig> ldapConfig = Optional.empty();

        private Optional<HdfConfigs> hdfConfigs = Optional.empty();

        private Optional<BaseFileSystemConfigurationsView> fileSystemView = Optional.empty();

        private GatewayView gatewayView;

        private Optional<KerberosConfig> kerberosConfig = Optional.empty();

        private Optional<FlexSubscription> flexSubscription = Optional.empty();

        private Optional<SharedServiceConfigsView> sharedServiceConfigs = Optional.empty();

        private GeneralClusterConfigs generalClusterConfigs;

        private BlueprintView blueprintView;

        private Map<String, Object> customInputs = new HashMap<>();

        private Map<String, Object> fixInputs = new HashMap<>();

        public static Builder builder() {
            return new Builder();
        }

        public Builder withSmartSenseSubscriptionId(String smartSenseSubscriptionId) {
            this.smartSenseSubscriptionId = Optional.ofNullable(smartSenseSubscriptionId);
            return this;
        }

        public Builder withRdsConfigs(Set<RDSConfig> rdsConfigs) {
            this.rdsConfigs = rdsConfigs;
            return this;
        }

        public Builder withHostgroups(Set<HostGroup> hostGroups) {
            for (HostGroup hostGroup : hostGroups) {
                InstanceGroup instanceGroup = hostGroup.getConstraint().getInstanceGroup();
                if (instanceGroup != null) {
                    this.hostgroupViews.add(new HostgroupView(hostGroup.getName(), 1,
                            instanceGroup.getInstanceGroupType(), instanceGroup.getNodeCount()));
                } else {
                    this.hostgroupViews.add(new HostgroupView(hostGroup.getName()));
                }

            }

            return this;
        }

        public Builder withHostgroupViews(Set<HostgroupView> hostgroupViews) {
            this.hostgroupViews = hostgroupViews;
            return this;
        }

        public Builder withStackRepoDetailsHdpVersion(String stackRepoDetailsHdpVersion) {
            this.stackRepoDetailsHdpVersion = Optional.ofNullable(stackRepoDetailsHdpVersion);
            return this;
        }

        public Builder withFileSystemConfigurationView(BaseFileSystemConfigurationsView fileSystemView) {
            this.fileSystemView =  Optional.ofNullable(fileSystemView);
            return this;
        }

        public Builder withLdapConfig(LdapConfig ldapConfig) {
            this.ldapConfig = Optional.ofNullable(ldapConfig);
            return this;
        }

        public Builder withHdfConfigs(HdfConfigs hdfConfigs) {
            this.hdfConfigs = Optional.ofNullable(hdfConfigs);
            return this;
        }

        public Builder withKerberosConfig(KerberosConfig kerberosConfig) {
            this.kerberosConfig = Optional.ofNullable(kerberosConfig);
            return this;
        }

        public Builder withGateway(Gateway gateway) {
            if (gateway != null) {
                this.gatewayView = new GatewayView(gateway);
            }
            return this;
        }

        public Builder withGatewayView(GatewayView gatewayView) {
            this.gatewayView = gatewayView;
            return this;
        }

        public Builder withBlueprintView(BlueprintView blueprintView) {
            this.blueprintView = blueprintView;
            return this;
        }

        public Builder withGeneralClusterConfigs(GeneralClusterConfigs generalClusterConfigs) {
            this.generalClusterConfigs = generalClusterConfigs;
            return this;
        }

        public Builder withFlexSubscription(FlexSubscription flexSubscription) {
            this.flexSubscription = Optional.ofNullable(flexSubscription);
            return this;
        }

        public Builder withSharedServiceConfigs(SharedServiceConfigsView sharedServiceConfigsView) {
            this.sharedServiceConfigs = Optional.ofNullable(sharedServiceConfigsView);
            return this;
        }

        public Builder withCustomInputs(Map<String, Object> customInputs) {
            this.customInputs = customInputs == null ? new HashMap<>() : customInputs;
            return this;
        }

        public Builder withFixInputs(Map<String, Object> fixInputs) {
            this.fixInputs = fixInputs == null ? new HashMap<>() : fixInputs;
            return this;
        }

        public BlueprintPreparationObject build() {
            return new BlueprintPreparationObject(this);
        }
    }
}
