package com.sequenceiq.cloudbreak.domain.environment;

import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.json.JsonStringSetUtils;
import com.sequenceiq.cloudbreak.domain.json.JsonToString;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.domain.workspace.WorkspaceAwareResource;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "name"}))
public class Environment implements WorkspaceAwareResource {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "environment_generator")
    @SequenceGenerator(name = "environment_generator", sequenceName = "environment_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000000, columnDefinition = "TEXT")
    private String description;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Workspace workspace;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Credential credential;

    @Column(nullable = false)
    private String cloudPlatform;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT", nullable = false)
    private Json regions;

    @ManyToMany(cascade = {CascadeType.MERGE}, fetch = FetchType.EAGER)
    @JoinTable(name = "env_ldap", joinColumns = @JoinColumn(name = "envid"), inverseJoinColumns = @JoinColumn(name = "ldapid"))
    private Set<LdapConfig> ldapConfigs;

    @ManyToMany(cascade = {CascadeType.MERGE}, fetch = FetchType.EAGER)
    @JoinTable(name = "env_proxy", joinColumns = @JoinColumn(name = "envid"), inverseJoinColumns = @JoinColumn(name = "proxyid"))
    private Set<ProxyConfig> proxyConfigs;

    @ManyToMany(cascade = {CascadeType.MERGE}, fetch = FetchType.EAGER)
    @JoinTable(name = "env_rds", joinColumns = @JoinColumn(name = "envid"), inverseJoinColumns = @JoinColumn(name = "rdsid"))
    private Set<RDSConfig> rdsConfigs;

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public Workspace getWorkspace() {
        return workspace;
    }

    @Override
    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    public Credential getCredential() {
        return credential;
    }

    public void setCredential(Credential credential) {
        this.credential = credential;
    }

    public Json getRegions() {
        return regions;
    }

    public void setRegions(Json regions) {
        this.regions = regions;
    }

    public Set<String> getRegionsSet() {
        return JsonStringSetUtils.jsonToStringSet(regions);
    }

    public void setRegionsSet(Set<String> regions) {
        this.regions = JsonStringSetUtils.stringSetToJson(regions);
    }

    public Set<LdapConfig> getLdapConfigs() {
        return ldapConfigs;
    }

    public void setLdapConfigs(Set<LdapConfig> ldapConfigs) {
        this.ldapConfigs = ldapConfigs;
    }

    public Set<ProxyConfig> getProxyConfigs() {
        return proxyConfigs;
    }

    public void setProxyConfigs(Set<ProxyConfig> proxyConfigs) {
        this.proxyConfigs = proxyConfigs;
    }

    public Set<RDSConfig> getRdsConfigs() {
        return rdsConfigs;
    }

    public void setRdsConfigs(Set<RDSConfig> rdsConfigs) {
        this.rdsConfigs = rdsConfigs;
    }

    @Override
    public WorkspaceResource getResource() {
        return WorkspaceResource.ENVIRONMENT;
    }

    @Override
    public String getOwner() {
        return null;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }
}
