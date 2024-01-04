package com.sequenceiq.cloudbreak.domain.stack.cluster;

import java.util.Optional;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.domain.ProvisionEntity;
import com.sequenceiq.cloudbreak.service.secret.SecretValue;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretToString;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.model.WorkspaceAwareResource;

@Entity
public class IdBroker implements ProvisionEntity, WorkspaceAwareResource {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "idbroker_generator")
    @SequenceGenerator(name = "idbroker_generator", sequenceName = "idbroker_id_seq", allocationSize = 1)
    private Long id;

    @OneToOne
    private Cluster cluster;

    @ManyToOne
    private Workspace workspace;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret masterSecret = Secret.EMPTY;

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

    public IdBroker copy() {
        IdBroker copy = new IdBroker();
        copy.id = id;
        copy.cluster = cluster;
        copy.workspace = workspace;
        copy.signCert = signCert;
        copy.signCertSecret = signCertSecret;
        copy.signPub = signPub;
        copy.signPubSecret = signPubSecret;
        copy.signKey = signKey;
        copy.masterSecret = masterSecret;

        return copy;
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
        return "idbroker-" + id;
    }

    @Override
    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
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

    public String getMasterSecret() {
        return masterSecret.getRaw();
    }

    public void setMasterSecret(String masterSecret) {
        this.masterSecret = new Secret(masterSecret);
    }

    public String getSignKey() {
        return signKey.getRaw();
    }

    public Secret getSignKeySecret() {
        return signKey;
    }

    public Secret getSignCertSecret() {
        return signCertSecret;
    }

    public Secret getSignPubSecret() {
        return signPubSecret;
    }

    @Deprecated
    public String getSignCertDeprecated() {
        return signCert;
    }

    @Deprecated
    public String getSignPubDeprecated() {
        return signPub;
    }

    @Deprecated
    public void setSignCertDeprecated(String signCertDeprecated) {
        this.signCert = signCertDeprecated;
    }

    @Deprecated
    public void setSignPubDeprecated(String signPubDeprecated) {
        this.signPub = signPubDeprecated;
    }

    public void setSignKey(String signKey) {
        this.signKey = new Secret(signKey);
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
        //remove this in future releases
        this.signPub = signPub;
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
        //remove this in future releases
        this.signCert = signCert;
    }

}
