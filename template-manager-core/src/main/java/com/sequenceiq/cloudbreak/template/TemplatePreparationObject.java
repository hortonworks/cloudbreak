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
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupRequest;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.domain.VolumeUsageType;
import com.sequenceiq.cloudbreak.domain.stack.cluster.IdBroker;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.dto.LdapView;
import com.sequenceiq.cloudbreak.dto.TrustView;
import com.sequenceiq.cloudbreak.template.filesystem.BaseFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.views.AccountMappingView;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.ClusterExposedServiceView;
import com.sequenceiq.cloudbreak.template.views.CustomConfigurationsView;
import com.sequenceiq.cloudbreak.template.views.DatalakeView;
import com.sequenceiq.cloudbreak.template.views.GatewayView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.template.views.PlacementView;
import com.sequenceiq.cloudbreak.template.views.ProductDetailsView;
import com.sequenceiq.cloudbreak.template.views.RdsView;
import com.sequenceiq.cloudbreak.template.views.SharedServiceConfigsView;

public class TemplatePreparationObject {

    private final String crn;

    private final CloudPlatform cloudPlatform;

    private final String platformVariant;

    private final GatewayView gatewayView;

    private final GeneralClusterConfigs generalClusterConfigs;

    private final BlueprintView blueprintView;

    private final Map<String, RdsView> rdsViews;

    private final String rdsSslCertificateFilePath;

    private final Set<HostgroupView> hostgroupViews;

    private final Optional<LdapView> ldapConfig;

    private final Optional<SharedServiceConfigsView> sharedServiceConfigs;

    private final Optional<CustomConfigurationsView> customConfigurationsView;

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

    private final VirtualGroupRequest virtualGroupRequest;

    private final Optional<DatalakeView> datalakeView;

    private final IdBroker idBroker;

    private final Map<String, String> servicePrincipals;

    private final boolean enableSecretEncryption;

    private final Optional<TrustView> trustView;

    private TemplatePreparationObject(Builder builder) {
        cloudPlatform = builder.cloudPlatform;
        platformVariant = builder.platformVariant;
        rdsViews = builder.rdsViews.stream().collect(Collectors.toMap(
                rdsConfig -> rdsConfig.getType().toLowerCase(),
                Function.identity()
        ));
        rdsSslCertificateFilePath = builder.rdsSslCertificateFilePath;
        hostgroupViews = builder.hostgroupViews;
        ldapConfig = builder.ldapConfig;
        gatewayView = builder.gatewayView;
        fileSystemView = builder.fileSystemView;
        kerberosConfig = builder.kerberosConfig;
        blueprintView = builder.blueprintView;
        generalClusterConfigs = builder.generalClusterConfigs;
        sharedServiceConfigs = builder.sharedServiceConfigs;
        customConfigurationsView = builder.customConfigurationsView;
        customInputs = builder.customInputs;
        fixInputs = builder.fixInputs;
        accountMappingView = builder.accountMappingView;
        placementView = builder.placementView;
        defaultTags = builder.defaultTags;
        productDetailsView = builder.productDetailsView;
        exposedServices = builder.exposedServices;
        stackType = builder.stackType;
        virtualGroupRequest = builder.virtualGroupRequest;
        datalakeView = builder.datalakeView;
        idBroker = builder.idBroker;
        servicePrincipals = builder.servicePrincipals;
        enableSecretEncryption = builder.enableSecretEncryption;
        crn = builder.crn;
        trustView = builder.trustView;
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

    public String getPlatformVariant() {
        return platformVariant;
    }

    public Optional<CustomConfigurationsView> getCustomConfigurationsView() {
        return customConfigurationsView;
    }

    public Set<RdsView> getRdsViews() {
        return Set.copyOf(rdsViews.values());
    }

    public RdsView getRdsView(DatabaseType type) {
        return rdsViews.get(type.name().toLowerCase());
    }

    public String getRdsSslCertificateFilePath() {
        return rdsSslCertificateFilePath;
    }

    public Set<HostgroupView> getHostgroupViews() {
        return hostgroupViews;
    }

    public Optional<LdapView> getLdapConfig() {
        return ldapConfig;
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

    public VirtualGroupRequest getVirtualGroupRequest() {
        return virtualGroupRequest;
    }

    public Optional<DatalakeView> getDatalakeView() {
        return datalakeView;
    }

    public IdBroker getIdBroker() {
        return idBroker;
    }

    public Map<String, String> getServicePrincipals() {
        return servicePrincipals;
    }

    public boolean isEnableSecretEncryption() {
        return enableSecretEncryption;
    }

    public String getCrn() {
        return crn;
    }

    public Optional<TrustView> getTrustView() {
        return trustView;
    }

    public static class Builder {

        private CloudPlatform cloudPlatform;

        private String platformVariant;

        private Set<RdsView> rdsViews = new HashSet<>();

        private String rdsSslCertificateFilePath;

        private Set<HostgroupView> hostgroupViews = new HashSet<>();

        private Optional<LdapView> ldapConfig = Optional.empty();

        private Optional<BaseFileSystemConfigurationsView> fileSystemView = Optional.empty();

        private GatewayView gatewayView;

        private Optional<KerberosConfig> kerberosConfig = Optional.empty();

        private Optional<SharedServiceConfigsView> sharedServiceConfigs = Optional.empty();

        private Optional<CustomConfigurationsView> customConfigurationsView = Optional.empty();

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

        private VirtualGroupRequest virtualGroupRequest;

        private Optional<DatalakeView> datalakeView = Optional.empty();

        private IdBroker idBroker;

        private Map<String, String> servicePrincipals;

        private boolean enableSecretEncryption;

        private String crn;

        private Optional<TrustView> trustView;

        public static Builder builder() {
            return new Builder();
        }

        public Builder withCloudPlatform(CloudPlatform cloudPlatform) {
            this.cloudPlatform = cloudPlatform;
            return this;
        }

        public Builder withPlatformVariant(String platformVariant) {
            this.platformVariant = platformVariant;
            return this;
        }

        public Builder withRdsViews(Set<RdsView> rdsViews) {
            this.rdsViews = rdsViews;
            return this;
        }

        public Builder withRdsSslCertificateFilePath(String rdsSslCertificateFilePath) {
            this.rdsSslCertificateFilePath = rdsSslCertificateFilePath;
            return this;
        }

        public Builder withHostgroups(Set<HostGroup> hostGroups, Set<String> ephemeralVolumeWhichMustBeProvisioned) {
            for (HostGroup hostGroup : hostGroups) {
                InstanceGroup instanceGroup = hostGroup.getInstanceGroup();
                if (instanceGroup != null) {
                    Template template = instanceGroup.getTemplate();
                    int localSsdCount = getLocalSsdCount(template, ephemeralVolumeWhichMustBeProvisioned);
                    int localSsdSize = getLocalSsdSize(template, ephemeralVolumeWhichMustBeProvisioned);
                    TemporaryStorage temporaryStorage = template == null ? null : template.getTemporaryStorage();
                    int instanceStorageCount = 0;
                    int instanceStorageSize = 0;
                    int volumeCount = 0;
                    Set<VolumeTemplate> volumeTemplates = new HashSet<>();
                    if (template != null) {
                        instanceStorageCount = template.getInstanceStorageCount() == null ? 0 : template.getInstanceStorageCount();
                        instanceStorageSize = template.getInstanceStorageSize() == null ? 0 : template.getInstanceStorageSize();
                        volumeCount = getVolumeCount(template, ephemeralVolumeWhichMustBeProvisioned);
                        volumeTemplates = getVolumeTemplates(template);
                    }
                    Integer temporaryStorageVolumeCount = template == null ? null : Math.max(instanceStorageCount, localSsdCount);
                    Integer temporaryStorageVolumeSize = template == null ? null : Math.max(instanceStorageSize, localSsdSize);

                    hostgroupViews.add(new HostgroupView(
                            hostGroup.getName(),
                            volumeCount,
                            instanceGroup.getInstanceGroupType(),
                            getFqdns(instanceGroup),
                            volumeTemplates,
                            temporaryStorage,
                            temporaryStorageVolumeCount,
                            temporaryStorageVolumeSize)
                    );
                } else {
                    hostgroupViews.add(new HostgroupView(hostGroup.getName()));
                }

            }

            return this;
        }

        private  Set<VolumeTemplate> getVolumeTemplates(Template template) {
            return template == null ? Collections.emptySet() : template.getVolumeTemplates();
        }

        private int getVolumeCount(Template template, Set<String> ephemeralVolumeWhichMustBeProvisioned) {
            return template == null ? 1 : template.getVolumeTemplates().stream().
                    filter(volumeTemplate -> volumeTemplate.getUsageType() == VolumeUsageType.GENERAL
                            &&  !ephemeralVolumeWhichMustBeProvisioned.stream().anyMatch(e -> e.equalsIgnoreCase(volumeTemplate.getVolumeType())))
                    .mapToInt(VolumeTemplate::getVolumeCount)
                    .sum();
        }

        private Set<String> getFqdns(InstanceGroup instanceGroup) {
            return instanceGroup.getAllInstanceMetaData().stream()
                    .filter(instanceMetaData -> instanceMetaData.getDiscoveryFQDN() != null)
                    .map(InstanceMetaData::getDiscoveryFQDN)
                    .collect(Collectors.toSet());
        }

        private int getLocalSsdCount(Template template, Set<String> ephemeralVolumeWhichMustBeProvisioned) {
            return template.getVolumeTemplates().stream()
                    .filter(t -> ephemeralVolumeWhichMustBeProvisioned.stream().anyMatch(e -> e.equalsIgnoreCase(t.getVolumeType())))
                    .findFirst()
                    .map(VolumeTemplate::getVolumeCount)
                    .orElse(0);
        }

        private int getLocalSsdSize(Template template, Set<String> ephemeralVolumeWhichMustBeProvisioned) {
            return template.getVolumeTemplates().stream()
                    .filter(t -> ephemeralVolumeWhichMustBeProvisioned.stream().anyMatch(e -> e.equalsIgnoreCase(t.getVolumeType())))
                    .findFirst()
                    .map(VolumeTemplate::getVolumeSize)
                    .orElse(0);
        }

        public Builder withHostgroupViews(Set<HostgroupView> hostgroupViews) {
            this.hostgroupViews = hostgroupViews;
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

        public Builder withGateway(Gateway gateway, String signKey, Set<String> fullServiceList) {
            gatewayView = gateway != null ? new GatewayView(gateway, signKey, fullServiceList) : null;
            return this;
        }

        public Builder withGateway(com.sequenceiq.cloudbreak.view.GatewayView gateway, String signKey, Set<String> fullServiceList) {
            gatewayView = gateway != null ? new GatewayView(gateway, signKey, fullServiceList) : null;
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

        public Builder withCustomConfigurationsView(CustomConfigurationsView customConfigurationsView) {
            this.customConfigurationsView = Optional.ofNullable(customConfigurationsView);
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

        public Builder withVirtualGroupView(VirtualGroupRequest virtualGroupRequest) {
            this.virtualGroupRequest = virtualGroupRequest;
            return this;
        }

        public Builder withDataLakeView(DatalakeView dataLakeView) {
            this.datalakeView = Optional.ofNullable(dataLakeView);
            return this;
        }

        public Builder withIdBroker(IdBroker idBroker) {
            this.idBroker = idBroker;
            return this;
        }

        public Builder withServicePrincipals(Map<String, String> servicePrincipals) {
            this.servicePrincipals = servicePrincipals;
            return this;
        }

        public Builder withEnableSecretEncryption(boolean enableSecretEncryption) {
            this.enableSecretEncryption = enableSecretEncryption;
            return this;
        }

        public Builder withCrn(String crn) {
            this.crn = crn;
            return this;
        }

        public Builder withTrust(Optional<TrustView> trustView) {
            this.trustView = trustView;
            return this;
        }

        public TemplatePreparationObject build() {
            return new TemplatePreparationObject(this);
        }
    }

}
