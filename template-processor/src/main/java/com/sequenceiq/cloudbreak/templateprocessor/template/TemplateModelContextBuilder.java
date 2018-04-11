package com.sequenceiq.cloudbreak.templateprocessor.template;

import com.sequenceiq.cloudbreak.api.model.FileSystemType;
import com.sequenceiq.cloudbreak.domain.Gateway;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.templateprocessor.nifi.HdfConfigs;
import com.sequenceiq.cloudbreak.templateprocessor.template.views.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

public class TemplateModelContextBuilder {

    private final Map<String, String> customProperties = new HashMap<>();

    private Map<String, RdsView> rds = new HashMap<>();

    private Map<String, FileSystemView> fileSystemConfig = new HashMap<>();

    private Optional<LdapView> ldap = Optional.empty();

    private Optional<GatewayView> gateway = Optional.empty();

    private Optional<HdfConfigs> hdfConfigs = Optional.empty();

    private String clusterName;

    private String clusterAdminPassword;

    private String clusterAdminFirstname;

    private String clusterAdminLastname;

    private String adminEmail;

    private boolean enableKnoxGateway;

    private boolean containerExecutorType;

    private String stackType;

    private String stackVersion;

    private Integer llapNodeCount;

    public TemplateModelContextBuilder withEnableKnoxGateway(boolean enableKnoxGateway) {
        this.enableKnoxGateway = enableKnoxGateway;
        return this;
    }

    public TemplateModelContextBuilder withClusterAdminPassword(String clusterAdminPassword) {
        this.clusterAdminPassword = clusterAdminPassword;
        return this;
    }

    public TemplateModelContextBuilder withClusterAdminFirstname(String clusterAdminFirstname) {
        this.clusterAdminFirstname = clusterAdminFirstname;
        return this;
    }

    public TemplateModelContextBuilder withClusterAdminLastname(String clusterAdminLastname) {
        this.clusterAdminLastname = clusterAdminLastname;
        return this;
    }

    public TemplateModelContextBuilder withAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
        return this;
    }

    public TemplateModelContextBuilder withRdsConfigs(Iterable<RDSConfig> rdsConfigs) {
        for (RDSConfig rdsConfig : rdsConfigs) {
            if (rdsConfig != null) {
                RdsView rdsView = new RdsView(rdsConfig);
                String componentName = rdsConfig.getType().toLowerCase();
                this.rds.put(componentName, rdsView);
            }
        }
        return this;
    }

    public TemplateModelContextBuilder withFileSystemConfigs(FileSystemConfigurationView fileSystemConfigurationView) {
        if (fileSystemConfigurationView != null && fileSystemConfigurationView.getFileSystemConfiguration() != null) {
            FileSystemView fileSystemView = new FileSystemView(fileSystemConfigurationView.getFileSystemConfiguration());
            String componentName = FileSystemType.fromClass(fileSystemConfig.getClass()).name().toLowerCase();
            this.fileSystemConfig.put(componentName, fileSystemView);
        }
        return this;
    }

    public TemplateModelContextBuilder withLdap(LdapConfig ldapConfig) {
        ldap = Optional.ofNullable(ldapConfig == null ? null : new LdapView(ldapConfig));
        return this;
    }

    public TemplateModelContextBuilder withGateway(Gateway gatewayConfig) {
        gateway = Optional.ofNullable(gateway == null ? null : new GatewayView(gatewayConfig));
        return this;
    }

    public TemplateModelContextBuilder withGateway(GatewayView gatewayConfig) {
        gateway = Optional.ofNullable(gatewayConfig);
        return this;
    }

    public TemplateModelContextBuilder withClusterName(String clusterName) {
        this.clusterName = clusterName;
        return this;
    }

    public TemplateModelContextBuilder withCustomProperty(String key, String value) {
        customProperties.put(key, value);
        return this;
    }

    public TemplateModelContextBuilder withLlapNodeCounts(Integer llapNodeCount) {
        this.llapNodeCount = llapNodeCount;
        return this;
    }

    public TemplateModelContextBuilder withCustomProperties(Map<String, Object> customProperties) {
        for (Entry<String, Object> customProperty : customProperties.entrySet()) {
            withCustomProperty(customProperty.getKey(), customProperty.getValue().toString());
        }
        return this;
    }

    public TemplateModelContextBuilder withContainerExecutor(boolean containerExecutorType) {
        this.containerExecutorType = containerExecutorType;
        return this;
    }

    public TemplateModelContextBuilder withStackType(String stackType) {
        this.stackType = stackType.toUpperCase();
        return this;
    }

    public TemplateModelContextBuilder withStackVersion(String stackVersion) {
        this.stackVersion = stackVersion;
        return this;
    }

    public TemplateModelContextBuilder withHdfConfigs(Optional<HdfConfigs> hdfConfigs) {
        this.hdfConfigs = hdfConfigs;
        return this;
    }

    public Map<String, Object> build() {
        Map<String, Object> blueprintTemplateModelContext = new HashMap<>();
        blueprintTemplateModelContext.put("ldap", ldap.orElse(null));
        blueprintTemplateModelContext.put("gateway", gateway.orElse(null));
        blueprintTemplateModelContext.put("rds", rds);
        blueprintTemplateModelContext.put("fileSystemConfigs", fileSystemConfig);
        for (Entry<String, String> customEntry : customProperties.entrySet()) {
            blueprintTemplateModelContext.put(customEntry.getKey(), customEntry.getValue());
        }
        blueprintTemplateModelContext.put("cluster_name", clusterName);
        blueprintTemplateModelContext.put("cluster_admin_password", clusterAdminPassword);
        blueprintTemplateModelContext.put("cluster_admin_firstname", clusterAdminFirstname);
        blueprintTemplateModelContext.put("cluster_admin_lastname", clusterAdminLastname);
        blueprintTemplateModelContext.put("admin_email", adminEmail);
        blueprintTemplateModelContext.put("enable_knox_gateway", enableKnoxGateway);
        blueprintTemplateModelContext.put("llap_node_count", llapNodeCount);
        blueprintTemplateModelContext.put("container_executor", containerExecutorType);
        blueprintTemplateModelContext.put("stack_type", stackType);
        blueprintTemplateModelContext.put("stack_type_version", stackVersion);
        blueprintTemplateModelContext.put("nifi_targets", hdfConfigs.isPresent() ? hdfConfigs.get().getNodeEntities() : null);
        blueprintTemplateModelContext.put("nifi_proxy_hosts", hdfConfigs.isPresent() ? hdfConfigs.get().getProxyHosts().orElse(null) : null);
        blueprintTemplateModelContext.put("stack_version", "{{stack_version}}");
        return blueprintTemplateModelContext;
    }
}
