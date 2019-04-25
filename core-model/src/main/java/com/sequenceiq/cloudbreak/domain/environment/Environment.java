package com.sequenceiq.cloudbreak.domain.environment;

import java.util.HashSet;
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
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Where;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.ArchivableResource;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.KubernetesConfig;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.json.JsonToString;
import com.sequenceiq.cloudbreak.domain.stack.cluster.DatalakeResources;
import com.sequenceiq.cloudbreak.domain.view.ClusterApiView;
import com.sequenceiq.cloudbreak.domain.view.StackApiView;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.domain.workspace.WorkspaceAwareResource;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Entity
@Where(clause = "archived = false")
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "name"}))
public class Environment implements WorkspaceAwareResource, ArchivableResource {

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

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private String locationDisplayName;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private Double latitude;

    private boolean archived;

    private Long deletionTimestamp = -1L;

    @ManyToMany(cascade = CascadeType.MERGE, fetch = FetchType.EAGER)
    @JoinTable(name = "env_ldap", joinColumns = @JoinColumn(name = "envid"), inverseJoinColumns = @JoinColumn(name = "ldapid"))
    private Set<LdapConfig> ldapConfigs = new HashSet<>();

    @ManyToMany(cascade = CascadeType.MERGE, fetch = FetchType.EAGER)
    @JoinTable(name = "env_proxy", joinColumns = @JoinColumn(name = "envid"), inverseJoinColumns = @JoinColumn(name = "proxyid"))
    private Set<ProxyConfig> proxyConfigs = new HashSet<>();

    @ManyToMany(cascade = CascadeType.MERGE, fetch = FetchType.EAGER)
    @JoinTable(name = "env_rds", joinColumns = @JoinColumn(name = "envid"), inverseJoinColumns = @JoinColumn(name = "rdsid"))
    private Set<RDSConfig> rdsConfigs = new HashSet<>();

    @ManyToMany(cascade = CascadeType.MERGE, fetch = FetchType.EAGER)
    @JoinTable(name = "env_kubernetes", joinColumns = @JoinColumn(name = "envid"), inverseJoinColumns = @JoinColumn(name = "kubernetesid"))
    private Set<KubernetesConfig> kubernetesConfigs = new HashSet<>();

    @ManyToMany(cascade = CascadeType.MERGE, fetch = FetchType.EAGER)
    @JoinTable(name = "env_kdc", joinColumns = @JoinColumn(name = "envid"), inverseJoinColumns = @JoinColumn(name = "kdcid"))
    private Set<KerberosConfig> kerberosConfigs = new HashSet<>();

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "environment_id")
    @Where(clause = "terminated IS NULL")
    private Set<StackApiView> stacks = new HashSet<>();

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "environment_id")
    @Where(clause = "status != 'DELETE_COMPLETED'")
    private Set<ClusterApiView> clusters = new HashSet<>();

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "environment_id")
    private Set<DatalakeResources> datalakeResources = new HashSet<>();

    @OneToOne(mappedBy = "environment", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private BaseNetwork network;

    public Environment() {
        try {
            regions = new Json(new HashSet<Region>());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

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

    @Override
    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    @Override
    public void unsetRelationsToEntitiesToBeDeleted() {
        ldapConfigs = null;
        rdsConfigs = null;
        proxyConfigs = null;
        kubernetesConfigs = null;
        kerberosConfigs = null;
        datalakeResources = null;
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

    public void setRegions(Set<Region> regions) {
        try {
            this.regions = new Json(regions);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public Set<Region> getRegionSet() {
        return JsonUtil.jsonToType(regions.getValue(), new TypeReference<>() {
        });
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

    public Set<KubernetesConfig> getKubernetesConfigs() {
        return kubernetesConfigs;
    }

    public void setKubernetesConfigs(Set<KubernetesConfig> kubernetesConfigs) {
        this.kubernetesConfigs = kubernetesConfigs;
    }

    public Set<StackApiView> getStacks() {
        return stacks;
    }

    public void setStacks(Set<StackApiView> stacks) {
        this.stacks = stacks;
    }

    public Set<ClusterApiView> getClusters() {
        return clusters;
    }

    public void setClusters(Set<ClusterApiView> clusters) {
        this.clusters = clusters;
    }

    public Set<DatalakeResources> getDatalakeResources() {
        return datalakeResources;
    }

    public void setDatalakeResources(Set<DatalakeResources> datalakeResources) {
        this.datalakeResources = datalakeResources;
    }

    @Override
    public WorkspaceResource getResource() {
        return WorkspaceResource.ENVIRONMENT;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public String getLocation() {
        return location;
    }

    public String getLocationDisplayName() {
        return locationDisplayName;
    }

    public void setLocationDisplayName(String locationDisplayName) {
        this.locationDisplayName = locationDisplayName;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Set<KerberosConfig> getKerberosConfigs() {
        return kerberosConfigs;
    }

    public void setKerberosConfigs(Set<KerberosConfig> kerberosConfigs) {
        this.kerberosConfigs = kerberosConfigs;
    }

    public boolean isArchived() {
        return archived;
    }

    public Long getDeletionTimestamp() {
        return deletionTimestamp;
    }

    public void setDeletionTimestamp(Long deletionTimestamp) {
        this.deletionTimestamp = deletionTimestamp;
    }

    public BaseNetwork getNetwork() {
        return network;
    }

    public void setNetwork(BaseNetwork network) {
        this.network = network;
    }
}
