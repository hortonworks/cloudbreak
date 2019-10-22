package com.sequenceiq.cloudbreak.template;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.dto.LdapView;
import com.sequenceiq.cloudbreak.template.filesystem.BaseFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.model.HdfConfigs;
import com.sequenceiq.cloudbreak.template.views.AccountMappingView;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.ClusterExposedServiceView;
import com.sequenceiq.cloudbreak.template.views.GatewayView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.template.views.PlacementView;
import com.sequenceiq.cloudbreak.template.views.ProductDetailsView;
import com.sequenceiq.cloudbreak.template.views.SharedServiceConfigsView;

public class TemplatePreparationObject {

    private final CloudPlatform cloudPlatform;

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

    private final Map<String, String> defaultTags;

    private final AccountMappingView accountMappingView;

    private final Optional<PlacementView> placementView;

    private final ProductDetailsView productDetailsView;

    private final Map<String, Collection<ClusterExposedServiceView>> exposedServices;

    private final StackType stackType;

    private TemplatePreparationObject(Builder builder) {
        cloudPlatform = builder.cloudPlatform;
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
        accountMappingView = builder.accountMappingView;
        placementView = builder.placementView;
        defaultTags = builder.defaultTags;
        productDetailsView = builder.productDetailsView;
        exposedServices = builder.exposedServices;
        stackType = builder.stackType;
    }

    public Stream<HostgroupView> getHostGroupsWithComponent(String component) {
        Set<String> groups = getBlueprintView().getProcessor()
                .getHostGroupsWithComponent(component);
        return getHostgroupViews().stream()
                .filter(hostGroup -> groups.contains(hostGroup.getName()));
    }

    public CloudPlatform getCloudPlatform() {
        return cloudPlatform;
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

    public AccountMappingView getAccountMappingView() {
        return accountMappingView;
    }

    public Optional<PlacementView> getPlacementView() {
        return placementView;
    }

    public Map<String, String> getDefaultTags() {
        return defaultTags;
    }

    public ProductDetailsView getProductDetailsView() {
        return productDetailsView;
    }

    public Map<String, Collection<ClusterExposedServiceView>> getExposedServices() {
        return exposedServices;
    }

    public StackType getStackType() {
        return stackType;
    }

    public static class Builder {

        private CloudPlatform cloudPlatform;

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

        private Map<String, String> defaultTags = new HashMap<>();

        private AccountMappingView accountMappingView;

        private Optional<PlacementView> placementView = Optional.empty();

        private ProductDetailsView productDetailsView;

        private StackType stackType;

        private Map<String, Collection<ClusterExposedServiceView>> exposedServices = new HashMap<>();

        public static Builder builder() {
            return new Builder();
        }

        public Builder withCloudPlatform(CloudPlatform cloudPlatform) {
            this.cloudPlatform = cloudPlatform;
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
                    Template template = instanceGroup.getTemplate();
                    int volumeCount = template == null ? 1 : template.getVolumeTemplates().stream()
                            .mapToInt(volume -> volume.getVolumeCount()).sum();
                    Set<VolumeTemplate> volumeTemplates = template == null ? Collections.EMPTY_SET
                            : template.getVolumeTemplates();
                    Set<String> fqdns = instanceGroup.getAllInstanceMetaData().stream()
                            .map(InstanceMetaData::getDiscoveryFQDN)
                            .collect(Collectors.toSet());
                    hostgroupViews.add(new HostgroupView(hostGroup.getName(), volumeCount,
                            instanceGroup.getInstanceGroupType(), fqdns, volumeTemplates));
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

        public Builder withProductDetails(ClouderaManagerRepo cm, List<ClouderaManagerProduct> products) {
            this.productDetailsView = new ProductDetailsView(cm, products == null ? new ArrayList<>() : products);
            return this;
        }

        public Builder withExposedServices(Map<String, Collection<ClusterExposedServiceView>> exposedServices) {
            this.exposedServices = exposedServices == null ? new HashMap<>() : exposedServices;
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

        public Builder withAccountMappingView(AccountMappingView accountMappingView) {
            this.accountMappingView = accountMappingView;
            return this;
        }

        public Builder withPlacementView(PlacementView placementView) {
            this.placementView = Optional.of(placementView);
            return this;
        }

        public Builder withDefaultTags(Map<String, String> defaultTags) {
            this.defaultTags = defaultTags == null ? new HashMap<>() : defaultTags;
            return this;
        }

        public Builder withStackType(StackType stackType) {
            this.stackType = stackType;
            return this;
        }

        public TemplatePreparationObject build() {
            return new TemplatePreparationObject(this);
        }
    }

}
