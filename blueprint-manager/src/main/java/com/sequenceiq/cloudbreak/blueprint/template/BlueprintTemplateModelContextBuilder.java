package com.sequenceiq.cloudbreak.blueprint.template;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import com.sequenceiq.cloudbreak.api.model.FileSystemType;
import com.sequenceiq.cloudbreak.blueprint.nifi.HdfConfigs;
import com.sequenceiq.cloudbreak.blueprint.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.blueprint.template.views.FileSystemConfigurationView;
import com.sequenceiq.cloudbreak.blueprint.template.views.FileSystemView;
import com.sequenceiq.cloudbreak.blueprint.template.views.GatewayView;
import com.sequenceiq.cloudbreak.blueprint.template.views.GeneralClusterConfigsView;
import com.sequenceiq.cloudbreak.blueprint.template.views.HdfConfigView;
import com.sequenceiq.cloudbreak.blueprint.template.views.LdapView;
import com.sequenceiq.cloudbreak.blueprint.template.views.RdsView;
import com.sequenceiq.cloudbreak.blueprint.template.views.SharedServiceConfigsView;
import com.sequenceiq.cloudbreak.blueprint.templates.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.domain.Gateway;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;

public class BlueprintTemplateModelContextBuilder {

    private final Map<String, String> customProperties = new HashMap<>();

    private Map<String, RdsView> rds = new HashMap<>();

    private Map<String, FileSystemView> fileSystemConfig = new HashMap<>();

    private Optional<LdapView> ldap = Optional.empty();

    private Optional<GatewayView> gateway = Optional.empty();

    private Optional<HdfConfigView> hdfConfigs = Optional.empty();

    private Optional<SharedServiceConfigsView> sharedServiceConfigs = Optional.empty();

    private GeneralClusterConfigsView generalClusterConfigsView;

    private BlueprintView blueprintView;

    private Set<String> components = new HashSet<>();

    public BlueprintTemplateModelContextBuilder withGeneralClusterConfigs(GeneralClusterConfigs generalClusterConfigs) {
        this.generalClusterConfigsView = new GeneralClusterConfigsView(generalClusterConfigs);
        return this;
    }

    public BlueprintTemplateModelContextBuilder withRdsConfigs(Iterable<RDSConfig> rdsConfigs) {
        for (RDSConfig rdsConfig : rdsConfigs) {
            if (rdsConfig != null) {
                RdsView rdsView = new RdsView(rdsConfig);
                String componentName = rdsConfig.getType().toLowerCase();
                this.rds.put(componentName, rdsView);
            }
        }
        return this;
    }

    public BlueprintTemplateModelContextBuilder withFileSystemConfigs(FileSystemConfigurationView fileSystemConfigurationView) {
        if (fileSystemConfigurationView != null && fileSystemConfigurationView.getFileSystemConfiguration() != null) {
            FileSystemView fileSystemView = new FileSystemView(fileSystemConfigurationView.getFileSystemConfiguration());
            String componentName = FileSystemType.fromClass(fileSystemConfig.getClass()).name().toLowerCase();
            this.fileSystemConfig.put(componentName, fileSystemView);
        }
        return this;
    }

    public BlueprintTemplateModelContextBuilder withLdap(LdapConfig ldapConfig) {
        this.ldap = Optional.ofNullable(ldapConfig == null ? null : new LdapView(ldapConfig));
        return this;
    }

    public BlueprintTemplateModelContextBuilder withGateway(Gateway gatewayConfig) {
        this.gateway = Optional.ofNullable(gatewayConfig == null ? null : new GatewayView(gatewayConfig));
        return this;
    }

    public BlueprintTemplateModelContextBuilder withBlueprintView(BlueprintView blueprintView) {
        this.blueprintView = blueprintView;
        return this;
    }

    public BlueprintTemplateModelContextBuilder withGateway(GatewayView gatewayConfig) {
        this.gateway = Optional.ofNullable(gatewayConfig);
        return this;
    }

    public BlueprintTemplateModelContextBuilder withCustomProperty(String key, String value) {
        this.customProperties.put(key, value);
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
        this.sharedServiceConfigs = Optional.ofNullable(sharedServiceConfigsView);
        return this;
    }

    public BlueprintTemplateModelContextBuilder withComponents(Set<String> components) {
        this.components = components;
        return this;
    }

    public Map<String, Object> build() {
        Map<String, Object> blueprintTemplateModelContext = new HashMap<>();
        blueprintTemplateModelContext.put(HandleBarModelKey.COMPONENTS.modelKey(), components);
        blueprintTemplateModelContext.put(HandleBarModelKey.LDAP.modelKey(), ldap.orElse(null));
        blueprintTemplateModelContext.put(HandleBarModelKey.GATEWAY.modelKey(), gateway.orElse(null));
        blueprintTemplateModelContext.put(HandleBarModelKey.RDS.modelKey(), rds);
        blueprintTemplateModelContext.put(HandleBarModelKey.FILESYSTEMCONFIGS.modelKey(), fileSystemConfig);
        blueprintTemplateModelContext.put(HandleBarModelKey.SHAREDSERVICE.modelKey(), sharedServiceConfigs.orElse(null));
        for (Entry<String, String> customEntry : customProperties.entrySet()) {
            blueprintTemplateModelContext.put(customEntry.getKey(), customEntry.getValue());
        }
        if (blueprintView != null) {
            for (Entry<String, Object> stringObjectEntry : blueprintView.getBlueprintInputs().entrySet()) {
                blueprintTemplateModelContext.put(stringObjectEntry.getKey(), stringObjectEntry.getValue());
            }
        }

        blueprintTemplateModelContext.put(HandleBarModelKey.BLUEPRINT.modelKey(), blueprintView);
        blueprintTemplateModelContext.put(HandleBarModelKey.HDF.modelKey(), hdfConfigs.orElse(null));
        blueprintTemplateModelContext.put(HandleBarModelKey.GENERAL.modelKey(), generalClusterConfigsView);
        blueprintTemplateModelContext.put(HandleBarModelKey.STACK_VERSION.modelKey(), "{{stack_version}}");
        return blueprintTemplateModelContext;
    }
}
