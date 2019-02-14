package com.sequenceiq.cloudbreak.template;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.sequenceiq.cloudbreak.domain.FlexSubscription;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.SmartSenseSubscription;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.template.filesystem.BaseFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.model.HdfConfigs;
import com.sequenceiq.cloudbreak.template.views.ClusterDefinitionView;
import com.sequenceiq.cloudbreak.template.views.GatewayView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.template.views.LdapView;
import com.sequenceiq.cloudbreak.template.views.SharedServiceConfigsView;
import com.sequenceiq.cloudbreak.template.views.SmartSenseSubscriptionView;

public class TemplatePreparationObject {

    private final GatewayView gatewayView;

    private final GeneralClusterConfigs generalClusterConfigs;

    private final ClusterDefinitionView clusterDefinitionView;

    private final Set<RDSConfig> rdsConfigs;

    private final Set<HostgroupView> hostgroupViews;

    private final Optional<SmartSenseSubscriptionView> smartSenseSubscription;

    private final Optional<LdapView> ldapConfig;

    private final Optional<SharedServiceConfigsView> sharedServiceConfigs;

    private final Optional<String> stackRepoDetailsHdpVersion;

    private final Optional<HdfConfigs> hdfConfigs;

    private final Optional<BaseFileSystemConfigurationsView> fileSystemView;

    private final Optional<KerberosConfig> kerberosConfig;

    private final Optional<FlexSubscription> flexSubscription;

    private final Map<String, Object> customInputs;

    private final Map<String, Object> fixInputs;

    private TemplatePreparationObject(Builder builder) {
        rdsConfigs = builder.rdsConfigs;
        hostgroupViews = builder.hostgroupViews;
        stackRepoDetailsHdpVersion = builder.stackRepoDetailsHdpVersion;
        smartSenseSubscription = builder.smartSenseSubscription;
        ldapConfig = builder.ldapConfig;
        hdfConfigs = builder.hdfConfigs;
        gatewayView = builder.gatewayView;
        fileSystemView = builder.fileSystemView;
        kerberosConfig = builder.kerberosConfig;
        clusterDefinitionView = builder.clusterDefinitionView;
        generalClusterConfigs = builder.generalClusterConfigs;
        flexSubscription = builder.flexSubscription;
        sharedServiceConfigs = builder.sharedServiceConfigs;
        customInputs = builder.customInputs;
        fixInputs = builder.fixInputs;
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

    public Optional<SmartSenseSubscriptionView> getSmartSenseSubscription() {
        return smartSenseSubscription;
    }

    public Optional<LdapView> getLdapConfig() {
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

    public ClusterDefinitionView getClusterDefinitionView() {
        return clusterDefinitionView;
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

        private Optional<SmartSenseSubscriptionView> smartSenseSubscription = Optional.empty();

        private Optional<LdapView> ldapConfig = Optional.empty();

        private Optional<HdfConfigs> hdfConfigs = Optional.empty();

        private Optional<BaseFileSystemConfigurationsView> fileSystemView = Optional.empty();

        private GatewayView gatewayView;

        private Optional<KerberosConfig> kerberosConfig = Optional.empty();

        private Optional<FlexSubscription> flexSubscription = Optional.empty();

        private Optional<SharedServiceConfigsView> sharedServiceConfigs = Optional.empty();

        private GeneralClusterConfigs generalClusterConfigs;

        private ClusterDefinitionView clusterDefinitionView;

        private Map<String, Object> customInputs = new HashMap<>();

        private Map<String, Object> fixInputs = new HashMap<>();

        public static Builder builder() {
            return new Builder();
        }

        public Builder withSmartSenseSubscription(SmartSenseSubscription smartSenseSubscription) {
            if (smartSenseSubscription != null) {
                this.smartSenseSubscription = Optional.of(new SmartSenseSubscriptionView(smartSenseSubscription));
            }
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
                    int volumeCount = instanceGroup.getTemplate() == null ? 1 : instanceGroup.getTemplate().getVolumeCount();
                    hostgroupViews.add(new HostgroupView(hostGroup.getName(), volumeCount,
                            instanceGroup.getInstanceGroupType(), instanceGroup.getNodeCount()));
                } else {
                    hostgroupViews.add(new HostgroupView(hostGroup.getName()));
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
            this.fileSystemView = Optional.ofNullable(fileSystemView);
            return this;
        }

        public Builder withLdapConfig(LdapConfig ldapConfig, String bindDn, String bindPassword) {
            this.ldapConfig = ldapConfig != null ? Optional.of(new LdapView(ldapConfig, bindDn, bindPassword)) : Optional.empty();
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

        public Builder withGateway(Gateway gateway, String signKey) {
            gatewayView = gateway != null ? new GatewayView(gateway, signKey) : null;
            return this;
        }

        public Builder withClusterDefinitionView(ClusterDefinitionView clusterDefinitionView) {
            this.clusterDefinitionView = clusterDefinitionView;
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
            sharedServiceConfigs = Optional.ofNullable(sharedServiceConfigsView);
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

        public TemplatePreparationObject build() {
            return new TemplatePreparationObject(this);
        }
    }
}