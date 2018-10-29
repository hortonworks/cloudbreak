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
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.api.model.GatewayType;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.SSOType;
import com.sequenceiq.cloudbreak.aspect.vault.VaultValue;
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;
import com.sequenceiq.cloudbreak.domain.converter.EncryptionConverter;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.json.JsonToString;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;

@Entity
public class Gateway implements ProvisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "gateway_generator")
    @SequenceGenerator(name = "gateway_generator", sequenceName = "gateway_id_seq", allocationSize = 1)
    private Long id;

    @OneToOne
    private Cluster cluster;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private GatewayType gatewayType = GatewayType.INDIVIDUAL;

    @Column(nullable = false)
    private String path;

    @OneToMany(mappedBy = "gateway", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<GatewayTopology> topologies = new HashSet<>();

    @Column(nullable = false)
    private String topologyName = "";

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json exposedServices;

    @VaultValue
    private String knoxMasterSecret;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SSOType ssoType = SSOType.NONE;

    private String ssoProvider;

    @Convert(converter = EncryptionConverter.class)
    private String signKey;

    private String signPub;

    private String signCert;

    private String tokenCert;

    // It is not used anyomore, other than to support Cloudbreak upgrade-ability (e.g. frm 2.4 to 2.7)
    // It is set to false by hibernate when loading old gateways created with previous CB versions
    private boolean enableGateway = true;

    public Gateway copy() {
        Gateway gateway = new Gateway();
        gateway.setTopologies(topologies.stream().map(GatewayTopology::copy).collect(Collectors.toSet()));
        gateway.setTokenCert(tokenCert);
        gateway.setSignCert(signCert);
        gateway.setSignKey(signKey);
        gateway.setPath(path);
        gateway.setGatewayType(gatewayType);
        gateway.setSsoType(ssoType);
        gateway.setCluster(cluster);
        gateway.setId(id);
        gateway.setSignPub(signPub);
        gateway.setSsoProvider(ssoProvider);
        return gateway;
    }

    public Long getId() {
        return id;
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
        return signKey;
    }

    public void setSignKey(String signKey) {
        this.signKey = signKey;
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
        return knoxMasterSecret;
    }

    public void setKnoxMasterSecret(String knoxMasterSecret) {
        this.knoxMasterSecret = knoxMasterSecret;
    }

    public Set<GatewayTopology> getTopologies() {
        if (StringUtils.isNotEmpty(topologyName) && topologies.stream().noneMatch(t -> t.getTopologyName().equals(topologyName))) {
            GatewayTopology gatewayTopology = new GatewayTopology();
            gatewayTopology.setTopologyName(topologyName);
            if (exposedServices != null && StringUtils.isNoneEmpty(exposedServices.getValue())) {
                gatewayTopology.setExposedServices(exposedServices);
            }
            topologies.add(gatewayTopology);

        }
        return topologies;
    }

    public void setTopologies(Set<GatewayTopology> topologies) {
        this.topologies = topologies;
    }

    // to support Cloudbreak upgrade-ability (e.g. frm 2.4 to 2.7)
    public boolean isGatewayEnabled() {
        return enableGateway;
    }
}
