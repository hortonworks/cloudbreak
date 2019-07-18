package com.sequenceiq.cloudbreak.template;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.dto.LdapView;
import com.sequenceiq.cloudbreak.template.filesystem.BaseFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.model.HdfConfigs;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.GatewayView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.template.views.SharedServiceConfigsView;

public class TemplatePreparationObject {

    private final GatewayView gatewayView;

    private final GeneralClusterConfigs generalClusterConfigs;

    private final BlueprintView blueprintView;

    private final Map<String, RDSConfig> rdsConfigs;

    private final Set<HostgroupView> hostgroupViews;

    private final Optional<LdapView> ldapConfig;

    private final Optional<SharedServiceConfigsView> sharedServiceConfigs;

    private final Optional<String> stackRepoDetailsHdpVersion;

    private final Optional<HdfConfigs> hdfConfigs;

    private final Optional<BaseFileSystemConfigurationsView> fileSystemView;

    private final Optional<KerberosConfig> kerberosConfig;

    private final Map<String, Object> customInputs;

    private final Map<String, Object> fixInputs;

    private final Map<String, String> identityGroupMapping;

    private final Map<String, String> identityUserMapping;

    private TemplatePreparationObject(Builder builder) {
        rdsConfigs = builder.rdsConfigs.stream().collect(Collectors.toMap(
                rdsConfig -> rdsConfig.getType().toLowerCase(),
                Function.identity()
        ));
        hostgroupViews = builder.hostgroupViews;
        stackRepoDetailsHdpVersion = builder.stackRepoDetailsHdpVersion;
        ldapConfig = builder.ldapConfig;
        hdfConfigs = builder.hdfConfigs;
        gatewayView = builder.gatewayView;
        fileSystemView = builder.fileSystemView;
        kerberosConfig = builder.kerberosConfig;
        blueprintView = builder.blueprintView;
        generalClusterConfigs = builder.generalClusterConfigs;
        sharedServiceConfigs = builder.sharedServiceConfigs;
        customInputs = builder.customInputs;
        fixInputs = builder.fixInputs;
        identityGroupMapping = builder.identityGroupMapping;
        identityUserMapping = builder.identityUserMapping;
    }

    public Stream<HostgroupView> getHostGroupsWithComponent(String component) {
        Set<String> groups = getBlueprintView().getProcessor()
                .getHostGroupsWithComponent(component);
        return getHostgroupViews().stream()
                .filter(hostGroup -> groups.contains(hostGroup.getName()));
    }

    public Set<RDSConfig> getRdsConfigs() {
        return Set.copyOf(rdsConfigs.values());
    }

    public RDSConfig getRdsConfig(DatabaseType type) {
        return rdsConfigs.get(type.name().toLowerCase());
    }

    public Set<HostgroupView> getHostgroupViews() {
        return hostgroupViews;
    }

    public Optional<String> getStackRepoDetailsHdpVersion() {
        return stackRepoDetailsHdpVersion;
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

        private Optional<LdapView> ldapConfig = Optional.empty();

        private Optional<HdfConfigs> hdfConfigs = Optional.empty();

        private Optional<BaseFileSystemConfigurationsView> fileSystemView = Optional.empty();

        private GatewayView gatewayView;

        private Optional<KerberosConfig> kerberosConfig = Optional.empty();

        private Optional<SharedServiceConfigsView> sharedServiceConfigs = Optional.empty();

        private GeneralClusterConfigs generalClusterConfigs;

        private BlueprintView blueprintView;

        private Map<String, Object> customInputs = new HashMap<>();

        private Map<String, Object> fixInputs = new HashMap<>();

        private Map<String, String> identityGroupMapping = new HashMap<>();

        private Map<String, String> identityUserMapping = new HashMap<>();

        public static Builder builder() {
            return new Builder();
        }

        public Builder withRdsConfigs(Set<RDSConfig> rdsConfigs) {
            this.rdsConfigs = rdsConfigs;
            return this;
        }

        public Builder withHostgroups(Set<HostGroup> hostGroups) {
            for (HostGroup hostGroup : hostGroups) {
                InstanceGroup instanceGroup = hostGroup.getConstraint().getInstanceGroup();
                if (instanceGroup != null) {
                    Template template = instanceGroup.getTemplate();
                    int volumeCount = template == null ? 1 : template.getVolumeTemplates().stream()
                            .mapToInt(volume -> volume.getVolumeCount()).sum();
                    Set<String> fqdns = instanceGroup.getAllInstanceMetaData().stream()
                            .map(InstanceMetaData::getDiscoveryFQDN)
                            .collect(Collectors.toSet());
                    hostgroupViews.add(new HostgroupView(hostGroup.getName(), volumeCount,
                            instanceGroup.getInstanceGroupType(), fqdns));
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

        public Builder withLdapConfig(LdapView ldapConfig) {
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

        public Builder withGateway(Gateway gateway, String signKey) {
            gatewayView = gateway != null ? new GatewayView(gateway, signKey) : null;
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

        public Builder withIdentityGroupMapping(Map<String, String> groupMapping) {
            identityGroupMapping = groupMapping == null ? new HashMap<>() : groupMapping;
            return this;
        }

        public Builder withIdentityUserMapping(Map<String, String> userMapping) {
            identityUserMapping = userMapping == null ? new HashMap<>() : userMapping;
            return this;
        }

        public TemplatePreparationObject build() {
            return new TemplatePreparationObject(this);
        }
    }
}
