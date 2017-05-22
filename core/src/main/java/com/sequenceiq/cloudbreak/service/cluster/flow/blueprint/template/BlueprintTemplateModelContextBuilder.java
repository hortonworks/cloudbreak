package com.sequenceiq.cloudbreak.service.cluster.flow.blueprint.template;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.model.AmbariDatabase;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.service.cluster.flow.blueprint.template.views.DatabaseView;
import com.sequenceiq.cloudbreak.service.cluster.flow.blueprint.template.views.LdapView;
import com.sequenceiq.cloudbreak.service.cluster.flow.blueprint.template.views.RdsView;

public class BlueprintTemplateModelContextBuilder {

    private Optional<RdsView> rangerRds = Optional.empty();

    private Optional<RdsView> hiveRds = Optional.empty();

    private Optional<RdsView> druidRds = Optional.empty();

    private Optional<LdapView> ldap = Optional.empty();

    private Optional<DatabaseView> ambariDatabase = Optional.empty();

    private String clusterName;

    private Map<String, String> customProperties = new HashMap<>();

    public BlueprintTemplateModelContextBuilder withRdsConfig(RDSConfig rdsConfig) {
        Optional<RdsView> rdsView = Optional.ofNullable(rdsConfig == null ? null : new RdsView(rdsConfig));
        switch (rdsConfig.getType()) {
            case HIVE:
                hiveRds = rdsView;
                break;
            case RANGER:
                rangerRds = rdsView;
                break;
            case DRUID:
                druidRds = rdsView;
                break;
            default:
                break;
        }
        return this;
    }

    public BlueprintTemplateModelContextBuilder withRdsConfigs(Set<RDSConfig> rdsConfigs) {
        for (RDSConfig rdsConfig : rdsConfigs) {
            withRdsConfig(rdsConfig);
        }
        return this;
    }

    public BlueprintTemplateModelContextBuilder withLdap(LdapConfig ldapConfig) {
        this.ldap = Optional.ofNullable(ldapConfig == null ? null : new LdapView(ldapConfig));
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
        this.customProperties.put(key, value);
        return this;
    }

    public BlueprintTemplateModelContextBuilder withCustomProperties(Map<String, Object> customProperties) {
        for (Map.Entry<String, Object> customProperty : customProperties.entrySet()) {
            withCustomProperty(customProperty.getKey(), customProperty.getValue().toString());
        }
        return this;
    }

    public Map<String, Object> build() {
        Map<String, Object> blueprintTemplateModelContext = new HashMap<>();
        blueprintTemplateModelContext.put("ldapConfig", this.ldap.orElse(null));
        blueprintTemplateModelContext.put("hiveRds", this.hiveRds.orElse(null));
        blueprintTemplateModelContext.put("rangerRds", this.rangerRds.orElse(null));
        blueprintTemplateModelContext.put("druidRds", this.druidRds.orElse(null));
        blueprintTemplateModelContext.put("ambariDatabase", this.ambariDatabase.orElse(null));
        for (Map.Entry<String, String> customEntry : this.customProperties.entrySet()) {
            blueprintTemplateModelContext.put(customEntry.getKey(), customEntry.getValue());
        }
        blueprintTemplateModelContext.put("cluster_name", this.clusterName);
        blueprintTemplateModelContext.put("stack_version", "{{stack_version}}");
        return blueprintTemplateModelContext;
    }

}