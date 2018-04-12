package com.sequenceiq.cloudbreak.template.processor.processor;


import com.sequenceiq.cloudbreak.domain.FlexSubscription;
import com.sequenceiq.cloudbreak.domain.Gateway;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.template.processor.templates.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.processor.nifi.HdfConfigs;
import com.sequenceiq.cloudbreak.template.processor.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.processor.template.views.FileSystemConfigurationView;
import com.sequenceiq.cloudbreak.template.processor.template.views.GatewayView;
import com.sequenceiq.cloudbreak.template.processor.template.views.HostgroupView;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class TemplatePreparationObject {

    private final GatewayView gatewayView;

    private final GeneralClusterConfigs generalClusterConfigs;

    private final BlueprintView blueprintView;

    private final Set<RDSConfig> rdsConfigs;

    private final Set<HostgroupView> hostgroupViews;

    private final Optional<String> smartSenseSubscriptionId;

    private final Optional<LdapConfig> ldapConfig;

    private final Optional<String> stackRepoDetailsHdpVersion;

    private final Optional<HdfConfigs> hdfConfigs;

    private final Optional<FileSystemConfigurationView> fileSystemView;

    private final Optional<KerberosConfig> kerberosConfig;

    private final Optional<FlexSubscription> flexSubscription;

    private TemplatePreparationObject(TemplatePreparationObject.Builder builder) {
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

    public Optional<FileSystemConfigurationView> getFileSystemConfigurationView() {
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

    public static class Builder {

        private Set<RDSConfig> rdsConfigs = new HashSet<>();

        private Set<HostgroupView> hostgroupViews = new HashSet<>();

        private Optional<String> stackRepoDetailsHdpVersion = Optional.empty();

        private Optional<String> smartSenseSubscriptionId = Optional.empty();

        private Optional<LdapConfig> ldapConfig = Optional.empty();

        private Optional<HdfConfigs> hdfConfigs = Optional.empty();

        private Optional<FileSystemConfigurationView> fileSystemView = Optional.empty();

        private GatewayView gatewayView;

        private Optional<KerberosConfig> kerberosConfig = Optional.empty();

        private Optional<FlexSubscription> flexSubscription = Optional.empty();

        private GeneralClusterConfigs generalClusterConfigs;

        private BlueprintView blueprintView;

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

        public Builder withFileSystemConfigurationView(FileSystemConfigurationView fileSystemView) {
            this.fileSystemView = Optional.ofNullable(fileSystemView);
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
            this.gatewayView = new GatewayView(gateway);
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

        public TemplatePreparationObject build() {
            return new TemplatePreparationObject(this);
        }
    }
}
