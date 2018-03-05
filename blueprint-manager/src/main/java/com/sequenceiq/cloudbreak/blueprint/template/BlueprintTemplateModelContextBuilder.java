package com.sequenceiq.cloudbreak.blueprint.template;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import com.sequenceiq.cloudbreak.cloud.model.AmbariDatabase;
import com.sequenceiq.cloudbreak.domain.Gateway;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.blueprint.template.views.DatabaseView;
import com.sequenceiq.cloudbreak.blueprint.template.views.GatewayView;
import com.sequenceiq.cloudbreak.blueprint.template.views.LdapView;
import com.sequenceiq.cloudbreak.blueprint.template.views.RdsView;

public class BlueprintTemplateModelContextBuilder {

    private final Map<String, String> customProperties = new HashMap<>();

    private Map<String, RdsView> rds = new HashMap<>();

    private Optional<LdapView> ldap = Optional.empty();

    private Optional<DatabaseView> ambariDatabase = Optional.empty();

    private Optional<GatewayView> gateway = Optional.empty();

    private String clusterName;

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

    public BlueprintTemplateModelContextBuilder withLdap(LdapConfig ldapConfig) {
        ldap = Optional.ofNullable(ldapConfig == null ? null : new LdapView(ldapConfig));
        return this;
    }

    public BlueprintTemplateModelContextBuilder withGateway(Gateway gatewayConfig) {
        gateway = Optional.ofNullable(gateway == null ? null : new GatewayView(gatewayConfig));
        return this;
    }

    public BlueprintTemplateModelContextBuilder withAmbariDatabase(AmbariDatabase ambariDatabase) {
        this.ambariDatabase = Optional.ofNullable(ambariDatabase == null ? null : new DatabaseView(ambariDatabase));
        return this;
    }

    public BlueprintTemplateModelContextBuilder withClusterName(String clusterName) {
        this.clusterName = clusterName;
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

    public Map<String, Object> build() {
        Map<String, Object> blueprintTemplateModelContext = new HashMap<>();
        blueprintTemplateModelContext.put("ldapConfig", ldap.orElse(null));
        blueprintTemplateModelContext.put("gateway", gateway.orElse(null));
        blueprintTemplateModelContext.put("rds", rds);
        blueprintTemplateModelContext.put("ambariDatabase", ambariDatabase.orElse(null));
        for (Entry<String, String> customEntry : customProperties.entrySet()) {
            blueprintTemplateModelContext.put(customEntry.getKey(), customEntry.getValue());
        }
        blueprintTemplateModelContext.put("cluster_name", clusterName);
        blueprintTemplateModelContext.put("stack_version", "{{stack_version}}");
        return blueprintTemplateModelContext;
    }

}