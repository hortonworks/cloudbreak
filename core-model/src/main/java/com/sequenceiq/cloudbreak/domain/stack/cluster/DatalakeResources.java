package com.sequenceiq.cloudbreak.domain.stack.cluster;

import java.util.Map;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.json.JsonStringSetUtils;
import com.sequenceiq.cloudbreak.domain.json.JsonToString;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.domain.workspace.WorkspaceAwareResource;

@Entity
public class DatalakeResources implements ProvisionEntity, WorkspaceAwareResource {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "datalakeresources_generator")
    @SequenceGenerator(name = "datalakeresources_generator", sequenceName = "datalakeresources_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne
    private Workspace workspace;

    @Column(name = "datalakestack_id")
    private Long datalakeStackId;

    private String name;

    private String datalakeAmbariUrl;

    private String datalakeAmbariIp;

    private String datalakeAmbariFqdn;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT", nullable = false)
    private Json datalakeComponents;

    @OneToMany(mappedBy = "datalakeResources", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @MapKey(name = "serviceName")
    private Map<String, ServiceDescriptor> serviceDescriptorMap;

    @ManyToMany(cascade = CascadeType.MERGE)
    private Set<RDSConfig> rdsConfigs;

    @ManyToOne
    private LdapConfig ldapConfig;

    @ManyToOne
    private KerberosConfig kerberosConfig;

    @ManyToOne
    private EnvironmentView environment;

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public Workspace getWorkspace() {
        return workspace;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    @Override
    public WorkspaceResource getResource() {
        return WorkspaceResource.STACK;
    }

    public Long getDatalakeStackId() {
        return datalakeStackId;
    }

    public void setDatalakeStackId(Long datalakeStackId) {
        this.datalakeStackId = datalakeStackId;
    }

    public String getDatalakeAmbariUrl() {
        return datalakeAmbariUrl;
    }

    public void setDatalakeAmbariUrl(String datalakeAmbariUrl) {
        this.datalakeAmbariUrl = datalakeAmbariUrl;
    }

    public String getDatalakeAmbariIp() {
        return datalakeAmbariIp;
    }

    public void setDatalakeAmbariIp(String datalakeAmbariIp) {
        this.datalakeAmbariIp = datalakeAmbariIp;
    }

    public String getDatalakeAmbariFqdn() {
        return datalakeAmbariFqdn;
    }

    public void setDatalakeAmbariFqdn(String datalakeAnbariFqdn) {
        datalakeAmbariFqdn = datalakeAnbariFqdn;
    }

    public Json getDatalakeComponents() {
        return datalakeComponents;
    }

    public void setDatalakeComponents(Json datalakeComponents) {
        this.datalakeComponents = datalakeComponents;
    }

    public Set<String> getDatalakeComponentSet() {
        return JsonStringSetUtils.jsonToStringSet(datalakeComponents);
    }

    public void setDatalakeComponentSet(Set<String> datalakeComponents) {
        this.datalakeComponents = JsonStringSetUtils.stringSetToJson(datalakeComponents);
    }

    public Map<String, ServiceDescriptor> getServiceDescriptorMap() {
        return serviceDescriptorMap;
    }

    public void setServiceDescriptorMap(Map<String, ServiceDescriptor> serviceDescriptorMap) {
        this.serviceDescriptorMap = serviceDescriptorMap;
    }

    public Set<RDSConfig> getRdsConfigs() {
        return rdsConfigs;
    }

    public void setRdsConfigs(Set<RDSConfig> rdsConfigs) {
        this.rdsConfigs = rdsConfigs;
    }

    public LdapConfig getLdapConfig() {
        return ldapConfig;
    }

    public void setLdapConfig(LdapConfig ldapConfig) {
        this.ldapConfig = ldapConfig;
    }

    public KerberosConfig getKerberosConfig() {
        return kerberosConfig;
    }

    public void setKerberosConfig(KerberosConfig kerberosConfig) {
        this.kerberosConfig = kerberosConfig;
    }

    public EnvironmentView getEnvironment() {
        return environment;
    }

    public void setEnvironment(EnvironmentView environment) {
        this.environment = environment;
    }
}
