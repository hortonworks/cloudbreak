package com.sequenceiq.cloudbreak.template;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.template.filesystem.BaseFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.model.HdfConfigs;
import com.sequenceiq.cloudbreak.template.utils.ModelConverterUtils;
import com.sequenceiq.cloudbreak.template.views.ClusterDefinitionView;
import com.sequenceiq.cloudbreak.template.views.GatewayView;
import com.sequenceiq.cloudbreak.template.views.GeneralClusterConfigsView;
import com.sequenceiq.cloudbreak.template.views.HdfConfigView;
import com.sequenceiq.cloudbreak.template.views.LdapView;
import com.sequenceiq.cloudbreak.template.views.RdsView;
import com.sequenceiq.cloudbreak.template.views.SharedServiceConfigsView;

public class TemplateModelContextBuilder {

    private final Map<String, String> customProperties = new HashMap<>();

    private final Map<String, RdsView> rds = new HashMap<>();

    private final Map<String, BaseFileSystemConfigurationsView> fileSystemConfig = new HashMap<>();

    private Optional<LdapView> ldap = Optional.empty();

    private Optional<KerberosConfig> kerberos = Optional.empty();

    private Optional<GatewayView> gateway = Optional.empty();

    private Optional<HdfConfigView> hdfConfigs = Optional.empty();

    private Optional<SharedServiceConfigsView> sharedServiceConfigs = Optional.empty();

    private GeneralClusterConfigsView generalClusterConfigsView;

    private ClusterDefinitionView clusterDefinitionView;

    private Set<String> components = new HashSet<>();

    private Map<String, Object> customInputs = new HashMap<>();

    private Map<String, Object> fixInputs = new HashMap<>();

    public TemplateModelContextBuilder withGeneralClusterConfigs(GeneralClusterConfigs generalClusterConfigs) {
        generalClusterConfigsView = new GeneralClusterConfigsView(generalClusterConfigs);
        return this;
    }

    public TemplateModelContextBuilder withRdsConfigs(Iterable<RDSConfig> rdsConfigs) {
        for (RDSConfig rdsConfig : rdsConfigs) {
            if (rdsConfig != null) {
                RdsView rdsView = new RdsView(rdsConfig);
                String componentName = rdsConfig.getType().toLowerCase();
                rds.put(componentName, rdsView);
            }
        }
        return this;
    }

    public TemplateModelContextBuilder withFileSystemConfigs(BaseFileSystemConfigurationsView fileSystemConfigurationView) {
        if (fileSystemConfigurationView != null) {
            withFileSystemConfigurationView(fileSystemConfigurationView);
        }
        return this;
    }

    private TemplateModelContextBuilder withFileSystemConfigurationView(BaseFileSystemConfigurationsView fileSystemConfigurationView) {
        String componentName = fileSystemConfigurationView.getType().toLowerCase();
        fileSystemConfig.put(componentName, fileSystemConfigurationView);
        return this;
    }

    public TemplateModelContextBuilder withLdap(LdapView ldapView) {
        ldap = Optional.ofNullable(ldapView);
        return this;
    }

    public TemplateModelContextBuilder withKerberos(KerberosConfig kerberosConfig) {
        kerberos = Optional.ofNullable(kerberosConfig);
        return this;
    }

    public TemplateModelContextBuilder withClusterDefinitionView(ClusterDefinitionView clusterDefinitionView) {
        this.clusterDefinitionView = clusterDefinitionView;
        return this;
    }

    public TemplateModelContextBuilder withGateway(Gateway gatewayConfig, String signKey) {
        gateway = Optional.ofNullable(gatewayConfig == null ? null : new GatewayView(gatewayConfig, signKey));
        return this;
    }

    public TemplateModelContextBuilder withGateway(GatewayView gatewayConfig) {
        gateway = Optional.ofNullable(gatewayConfig);
        return this;
    }

    public TemplateModelContextBuilder withCustomProperties(Map<String, Object> customProperties) {
        for (Entry<String, Object> customProperty : customProperties.entrySet()) {
            withCustomProperty(customProperty.getKey(), customProperty.getValue().toString());
        }
        return this;
    }

    public TemplateModelContextBuilder withCustomProperty(String key, String value) {
        customProperties.put(key, value);
        return this;
    }

    public TemplateModelContextBuilder withHdfConfigs(HdfConfigs hdfConfigs) {
        this.hdfConfigs = hdfConfigs == null ? Optional.empty() : Optional.of(new HdfConfigView(hdfConfigs));
        return this;
    }

    public TemplateModelContextBuilder withSharedServiceConfigs(SharedServiceConfigsView sharedServiceConfigsView) {
        sharedServiceConfigs = Optional.ofNullable(sharedServiceConfigsView);
        return this;
    }

    public TemplateModelContextBuilder withComponents(Set<String> components) {
        this.components = components;
        return this;
    }

    public TemplateModelContextBuilder withCustomInputs(Map<String, Object> customInputs) {
        if (customInputs != null) {
            this.customInputs = customInputs;
        }
        return this;
    }

    public TemplateModelContextBuilder withFixInputs(Map<String, Object> fixInputs) {
        if (fixInputs != null) {
            this.fixInputs = fixInputs;
        }
        return this;
    }

    public Map<String, Object> build() {
        Map<String, Object> templateModelContext = new HashMap<>();
        templateModelContext.put(HandleBarModelKey.COMPONENTS.modelKey(), components);
        templateModelContext.put(HandleBarModelKey.LDAP.modelKey(), ldap.orElse(null));
        templateModelContext.put(HandleBarModelKey.KERBEROS.modelKey(), kerberos.orElse(null));
        templateModelContext.put(HandleBarModelKey.GATEWAY.modelKey(), gateway.orElse(null));
        templateModelContext.put(HandleBarModelKey.RDS.modelKey(), rds);
        templateModelContext.put(HandleBarModelKey.FILESYSTEMCONFIGS.modelKey(), ModelConverterUtils.convert(createAdjustedFileSystemConfig()));
        templateModelContext.put(HandleBarModelKey.SHAREDSERVICE.modelKey(), sharedServiceConfigs.orElse(null));
        templateModelContext.put(HandleBarModelKey.CLUSTER_DEFINITION.modelKey(), clusterDefinitionView);
        templateModelContext.put(HandleBarModelKey.HDF.modelKey(), hdfConfigs.orElse(null));
        templateModelContext.put(HandleBarModelKey.GENERAL.modelKey(), generalClusterConfigsView);
        ModelConverterUtils.deepMerge(templateModelContext, ModelConverterUtils.convert(customInputs));
        ModelConverterUtils.deepMerge(templateModelContext, ModelConverterUtils.convert(fixInputs));
        templateModelContext.put(HandleBarModelKey.STACK_VERSION.modelKey(), "{{stack_version}}");
        return templateModelContext;
    }

    private Map<String, Object> createAdjustedFileSystemConfig() {
        Map<String, Object> result = new HashMap<>();
        result.putAll(fileSystemConfig);
        result.put("cloudStorageEnabled", !fileSystemConfig.isEmpty());
        return result;
    }
}
