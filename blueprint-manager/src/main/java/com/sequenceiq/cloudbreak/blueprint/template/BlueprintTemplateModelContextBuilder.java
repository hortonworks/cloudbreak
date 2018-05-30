package com.sequenceiq.cloudbreak.blueprint.template;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import com.sequenceiq.cloudbreak.blueprint.filesystem.BaseFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.blueprint.nifi.HdfConfigs;
import com.sequenceiq.cloudbreak.blueprint.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.blueprint.template.views.GatewayView;
import com.sequenceiq.cloudbreak.blueprint.template.views.GeneralClusterConfigsView;
import com.sequenceiq.cloudbreak.blueprint.template.views.HdfConfigView;
import com.sequenceiq.cloudbreak.blueprint.template.views.LdapView;
import com.sequenceiq.cloudbreak.blueprint.template.views.RdsView;
import com.sequenceiq.cloudbreak.blueprint.template.views.SharedServiceConfigsView;
import com.sequenceiq.cloudbreak.blueprint.templates.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.blueprint.utils.ModelConverterUtils;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;

public class BlueprintTemplateModelContextBuilder {

    private final Map<String, String> customProperties = new HashMap<>();

    private final Map<String, RdsView> rds = new HashMap<>();

    private final Map<String, BaseFileSystemConfigurationsView> fileSystemConfig = new HashMap<>();

    private Optional<LdapView> ldap = Optional.empty();

    private Optional<GatewayView> gateway = Optional.empty();

    private Optional<HdfConfigView> hdfConfigs = Optional.empty();

    private Optional<SharedServiceConfigsView> sharedServiceConfigs = Optional.empty();

    private GeneralClusterConfigsView generalClusterConfigsView;

    private BlueprintView blueprintView;

    private Set<String> components = new HashSet<>();

    private Map<String, Object> customInputs = new HashMap<>();

    private Map<String, Object> fixInputs = new HashMap<>();

    public BlueprintTemplateModelContextBuilder withGeneralClusterConfigs(GeneralClusterConfigs generalClusterConfigs) {
        generalClusterConfigsView = new GeneralClusterConfigsView(generalClusterConfigs);
        return this;
    }

    public BlueprintTemplateModelContextBuilder withRdsConfigs(Iterable<RDSConfig> rdsConfigs) {
        for (RDSConfig rdsConfig : rdsConfigs) {
            if (rdsConfig != null) {
                RdsView rdsView = new RdsView(rdsConfig);
                String componentName = rdsConfig.getType().toLowerCase();
                rds.put(componentName, rdsView);
            }
        }
        return this;
    }

    public BlueprintTemplateModelContextBuilder withFileSystemConfigs(BaseFileSystemConfigurationsView fileSystemConfigurationView) {
        if (fileSystemConfigurationView != null) {
            withFileSystemConfigurationView(fileSystemConfigurationView);
        }
        return this;
    }

    private BlueprintTemplateModelContextBuilder withFileSystemConfigurationView(BaseFileSystemConfigurationsView fileSystemConfigurationView) {
        String componentName = fileSystemConfigurationView.getType().toLowerCase();
        fileSystemConfig.put(componentName, fileSystemConfigurationView);
        return this;
    }

    public BlueprintTemplateModelContextBuilder withLdap(LdapConfig ldapConfig) {
        ldap = Optional.ofNullable(ldapConfig == null ? null : new LdapView(ldapConfig));
        return this;
    }

    public BlueprintTemplateModelContextBuilder withGateway(Gateway gatewayConfig) {
        gateway = Optional.ofNullable(gatewayConfig == null ? null : new GatewayView(gatewayConfig));
        return this;
    }

    public BlueprintTemplateModelContextBuilder withBlueprintView(BlueprintView blueprintView) {
        this.blueprintView = blueprintView;
        return this;
    }

    public BlueprintTemplateModelContextBuilder withGateway(GatewayView gatewayConfig) {
        gateway = Optional.ofNullable(gatewayConfig);
        return this;
    }

    public BlueprintTemplateModelContextBuilder withCustomProperty(String key, String value) {
        customProperties.put(key, value);
        return this;
    }

    public BlueprintTemplateModelContextBuilder withCustomProperties(Map<String, Object> customProperties) {
        for (Entry<String, Object> customProperty : customProperties.entrySet()) {
            withCustomProperty(customProperty.getKey(), customProperty.getValue().toString());
        }
        return this;
    }

    public BlueprintTemplateModelContextBuilder withHdfConfigs(HdfConfigs hdfConfigs) {
        if (hdfConfigs == null) {
            this.hdfConfigs = Optional.empty();
        } else {
            this.hdfConfigs = Optional.of(new HdfConfigView(hdfConfigs));
        }
        return this;
    }

    public BlueprintTemplateModelContextBuilder withSharedServiceConfigs(SharedServiceConfigsView sharedServiceConfigsView) {
        sharedServiceConfigs = Optional.ofNullable(sharedServiceConfigsView);
        return this;
    }

    public BlueprintTemplateModelContextBuilder withComponents(Set<String> components) {
        this.components = components;
        return this;
    }

    public BlueprintTemplateModelContextBuilder withCustomInputs(Map<String, Object> customInputs) {
        if (customInputs != null) {
            this.customInputs = customInputs;
        }
        return this;
    }

    public BlueprintTemplateModelContextBuilder withFixInputs(Map<String, Object> fixInputs) {
        if (fixInputs != null) {
            this.fixInputs = fixInputs;
        }
        return this;
    }

    public Map<String, Object> build() {
        Map<String, Object> blueprintTemplateModelContext = new HashMap<>();
        blueprintTemplateModelContext.put(HandleBarModelKey.COMPONENTS.modelKey(), components);
        blueprintTemplateModelContext.put(HandleBarModelKey.LDAP.modelKey(), ldap.orElse(null));
        blueprintTemplateModelContext.put(HandleBarModelKey.LDAP.modelKey(), ldap.orElse(null));
        blueprintTemplateModelContext.put(HandleBarModelKey.GATEWAY.modelKey(), gateway.orElse(null));
        blueprintTemplateModelContext.put(HandleBarModelKey.RDS.modelKey(), rds);
        blueprintTemplateModelContext.put(HandleBarModelKey.FILESYSTEMCONFIGS.modelKey(), ModelConverterUtils.convert(fileSystemConfig));
        blueprintTemplateModelContext.put(HandleBarModelKey.SHAREDSERVICE.modelKey(), sharedServiceConfigs.orElse(null));
        blueprintTemplateModelContext.put(HandleBarModelKey.BLUEPRINT.modelKey(), blueprintView);
        blueprintTemplateModelContext.put(HandleBarModelKey.HDF.modelKey(), hdfConfigs.orElse(null));
        blueprintTemplateModelContext.put(HandleBarModelKey.GENERAL.modelKey(), generalClusterConfigsView);
        ModelConverterUtils.deepMerge(blueprintTemplateModelContext, ModelConverterUtils.convert(customInputs));
        ModelConverterUtils.deepMerge(blueprintTemplateModelContext, ModelConverterUtils.convert(fixInputs));
        blueprintTemplateModelContext.put(HandleBarModelKey.STACK_VERSION.modelKey(), "{{stack_version}}");
        return blueprintTemplateModelContext;
    }
}
