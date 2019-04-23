package com.sequenceiq.cloudbreak.domain.stack.cluster.gateway;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.GatewayType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.SSOType;
import com.sequenceiq.cloudbreak.aspect.secret.SecretValue;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;
import com.sequenceiq.cloudbreak.domain.Secret;
import com.sequenceiq.cloudbreak.domain.SecretToString;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.json.JsonToString;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.domain.workspace.WorkspaceAwareResource;

@Entity
public class Gateway implements ProvisionEntity, WorkspaceAwareResource {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "gateway_generator")
    @SequenceGenerator(name = "gateway_generator", sequenceName = "gateway_id_seq", allocationSize = 1)
    private Long id;

    @OneToOne
    private Cluster cluster;

    @Enumerated(EnumType.STRING)
    private GatewayType gatewayType = GatewayType.INDIVIDUAL;

    private String path;

    @OneToMany(mappedBy = "gateway", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<GatewayTopology> topologies = new HashSet<>();

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json exposedServices;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret knoxMasterSecret = Secret.EMPTY;

    @Enumerated(EnumType.STRING)
    private SSOType ssoType = SSOType.NONE;

    private String ssoProvider;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret signKey = Secret.EMPTY;

    private String signPub;

    private String signCert;

    private String tokenCert;

    @ManyToOne
    private Workspace workspace;

    public Gateway copy() {
        Gateway gateway = new Gateway();
        gateway.topologies = topologies.stream().map(GatewayTopology::copy).collect(Collectors.toSet());
        gateway.tokenCert = tokenCert;
        gateway.signCert = signCert;
        gateway.signKey = signKey;
        gateway.path = path;
        gateway.gatewayType = gatewayType;
        gateway.ssoType = ssoType;
        gateway.cluster = cluster;
        gateway.id = id;
        gateway.signPub = signPub;
        gateway.ssoProvider = ssoProvider;
        gateway.workspace = workspace;
        return gateway;
    }

    public Long getId() {
        return id;
    }

    @Override
    public Workspace getWorkspace() {
        return workspace;
    }

    @Override
    public String getName() {
        return getResource().getShortName() + '-' + id;
    }

    @Override
    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    @Override
    public WorkspaceResource getResource() {
        return WorkspaceResource.STACK;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Cluster getCluster() {
        return cluster;
    }

    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }

    public GatewayType getGatewayType() {
        return gatewayType;
    }

    public void setGatewayType(GatewayType gatewayType) {
        this.gatewayType = gatewayType;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public SSOType getSsoType() {
        return ssoType;
    }

    public void setSsoType(SSOType ssoType) {
        this.ssoType = ssoType;
    }

    public String getSsoProvider() {
        return ssoProvider;
    }

    public void setSsoProvider(String ssoProvider) {
        this.ssoProvider = ssoProvider;
    }

    public String getSignKey() {
        return signKey.getRaw();
    }

    public void setSignKey(String signKey) {
        this.signKey = new Secret(signKey);
    }

    public String getSignCert() {
        return signCert;
    }

    public void setSignCert(String signCert) {
        this.signCert = signCert;
    }

    public String getSignPub() {
        return signPub;
    }

    public void setSignPub(String signPub) {
        this.signPub = signPub;
    }

    public String getTokenCert() {
        return tokenCert;
    }

    public void setTokenCert(String tokenCert) {
        this.tokenCert = tokenCert;
    }

    public String getKnoxMasterSecret() {
        return knoxMasterSecret.getRaw();
    }

    public void setKnoxMasterSecret(String knoxMasterSecret) {
        this.knoxMasterSecret = new Secret(knoxMasterSecret);
    }

    public Set<GatewayTopology> getTopologies() {
        return topologies;
    }

    public void setTopologies(Set<GatewayTopology> topologies) {
        this.topologies = topologies;
    }
}
