package com.sequenceiq.cloudbreak.domain.stack.cluster.gateway;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.GatewayType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.SSOType;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;
import com.sequenceiq.cloudbreak.domain.converter.GatewayTypeConverter;
import com.sequenceiq.cloudbreak.domain.converter.SSOTypeConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.secret.SecretGetter;
import com.sequenceiq.cloudbreak.service.secret.SecretMarker;
import com.sequenceiq.cloudbreak.service.secret.SecretSetter;
import com.sequenceiq.cloudbreak.service.secret.SecretValue;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretToString;
import com.sequenceiq.cloudbreak.view.GatewayView;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.model.WorkspaceAwareResource;

@Entity
public class Gateway implements ProvisionEntity, WorkspaceAwareResource, GatewayView {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "gateway_generator")
    @SequenceGenerator(name = "gateway_generator", sequenceName = "gateway_id_seq", allocationSize = 1)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    private Cluster cluster;

    @Convert(converter = GatewayTypeConverter.class)
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

    @Convert(converter = SSOTypeConverter.class)
    private SSOType ssoType = SSOType.NONE;

    private String ssoProvider;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret signKey = Secret.EMPTY;

    @Deprecated
    private String signPub;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret signPubSecret = Secret.EMPTY;

    @Deprecated
    private String signCert;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret signCertSecret = Secret.EMPTY;

    @Deprecated
    private String tokenCert;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret tokenCertSecret = Secret.EMPTY;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret tokenKeySecret = Secret.EMPTY;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret tokenPubSecret = Secret.EMPTY;

    @ManyToOne
    private Workspace workspace;

    private Integer gatewayPort;

    public Gateway copy() {
        Gateway gateway = new Gateway();
        gateway.topologies = topologies.stream().map(GatewayTopology::copy).collect(Collectors.toSet());
        gateway.tokenCert = tokenCert;
        gateway.tokenCertSecret = tokenCertSecret;
        gateway.tokenKeySecret = tokenKeySecret;
        gateway.tokenPubSecret = tokenPubSecret;
        gateway.signCert = signCert;
        gateway.signCertSecret = signCertSecret;
        gateway.signKey = signKey;
        gateway.path = path;
        gateway.gatewayType = gatewayType;
        gateway.ssoType = ssoType;
        gateway.cluster = cluster;
        gateway.id = id;
        gateway.signPub = signPub;
        gateway.signPubSecret = signPubSecret;
        gateway.ssoProvider = ssoProvider;
        gateway.workspace = workspace;
        gateway.gatewayPort = gatewayPort;
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
        return "gateway-" + id;
    }

    @Override
    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @deprecated This is only used by JPA. The cluster field from gateway was made lazy loading because it generated slow query.
     */
    @Deprecated
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

    @Override
    @SecretGetter(marker = SecretMarker.GATEWAY_SIGN_KEY)
    public Secret getSignKeySecret() {
        return signKey;
    }

    @Override
    @SecretGetter(marker = SecretMarker.GATEWAY_SIGN_CERT)
    public Secret getSignCertSecret() {
        return signCertSecret;
    }

    @Override
    @SecretGetter(marker = SecretMarker.GATEWAY_SIGN_PUB)
    public Secret getSignPubSecret() {
        return signPubSecret;
    }

    @Deprecated
    @Override
    public String getSignCertDeprecated() {
        return signCert;
    }

    @Deprecated
    public void setSignCertDeprecated(String signCert) {
        this.signCert = signCert;
    }

    @Deprecated
    @Override
    public String getSignPubDeprecated() {
        return signPub;
    }

    @Deprecated
    public void setSignPubDeprecated(String signPub) {
        this.signPub = signPub;
    }

    @Deprecated
    public void setTokenCertDeprecated(String tokenCert) {
        this.tokenCert = tokenCert;
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

    @SecretSetter(marker = SecretMarker.GATEWAY_SIGN_KEY)
    public void setSignKeySecret(Secret signKey) {
        this.signKey = signKey;
    }

    public String getSignCert() {
        return Optional.ofNullable(signCertSecret)
                .map(Secret::getRaw)
                .orElse(signCert);
    }

    public void setSignCert(String signCert) {
        if (signCert != null) {
            this.signCertSecret = new Secret(signCert);
        }
    }

    @SecretSetter(marker = SecretMarker.GATEWAY_SIGN_CERT)
    public void setSignCertSecret(Secret signCert) {
        if (signCert != null) {
            this.signCertSecret = signCert;
        }
    }

    public String getSignPub() {
        return Optional.ofNullable(signPubSecret)
                .map(Secret::getRaw)
                .orElse(signPub);
    }

    public void setSignPub(String signPub) {
        if (signPub != null) {
            this.signPubSecret = new Secret(signPub);
        }
    }

    @SecretSetter(marker = SecretMarker.GATEWAY_SIGN_PUB)
    public void setSignPubSecret(Secret signPub) {
        if (signPub != null) {
            this.signPubSecret = signPub;
        }
    }

    public String getTokenCert() {
        return Optional.ofNullable(tokenCertSecret)
                .map(Secret::getRaw)
                .orElse(tokenCert);
    }

    public void setTokenCert(String tokenCert) {
        setTokenCertSecret(tokenCert);
        this.tokenCert = tokenCert;
    }

    @SecretSetter(marker = SecretMarker.GATEWAY_TOKEN_CERT)
    public void setTokenCertSecretJson(Secret tokenCert) {
        this.tokenCert = tokenCert.getRaw();
        this.tokenCertSecret = tokenCert;
    }

    public void setTokenKeySecret(String tokenPrivateKey) {
        if (tokenPrivateKey != null) {
            this.tokenKeySecret = new Secret(tokenPrivateKey);
        }
    }

    @SecretSetter(marker = SecretMarker.GATEWAY_TOKEN_KEY)
    public void setTokenKeySecretJson(Secret tokenPrivateKey) {
        if (tokenPrivateKey != null) {
            this.tokenKeySecret = tokenPrivateKey;
        }
    }

    public void setTokenPubSecret(String tokenPublicKey) {
        if (tokenPublicKey != null) {
            this.tokenPubSecret = new Secret(tokenPublicKey);
        }
    }

    @SecretSetter(marker = SecretMarker.GATEWAY_TOKEN_PUB)
    public void setTokenPubSecretJson(Secret tokenPublicKey) {
        if (tokenPublicKey != null) {
            this.tokenPubSecret = tokenPublicKey;
        }
    }

    public void setTokenCertSecret(String tokenCert) {
        if (tokenCert != null) {
            this.tokenCertSecret = new Secret(tokenCert);
        }
    }

    @SecretGetter(marker = SecretMarker.GATEWAY_TOKEN_CERT)
    public Secret getTokenCertSecret() {
        return tokenCertSecret;
    }

    @Deprecated
    public String getTokenCertDeprecated() {
        return tokenCert;
    }

    @SecretGetter(marker = SecretMarker.GATEWAY_TOKEN_PUB)
    public Secret getTokenPubSecret() {
        return tokenPubSecret;
    }

    @SecretGetter(marker = SecretMarker.GATEWAY_TOKEN_KEY)
    public Secret getTokenKeySecret() {
        return tokenKeySecret;
    }

    public String getTokenKeyPath() {
        return tokenKeySecret.getSecret();
    }

    @Override
    public Secret getKnoxMasterSecret() {
        return knoxMasterSecret;
    }

    public String getKnoxMaster() {
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

    public Integer getGatewayPort() {
        return gatewayPort;
    }

    public void setGatewayPort(Integer gatewayPort) {
        this.gatewayPort = gatewayPort;
    }

    public Json getExposedServices() {
        return exposedServices;
    }

    public void setExposedServices(Json exposedServices) {
        this.exposedServices = exposedServices;
    }
}
