package com.sequenceiq.cloudbreak.domain.stack.cluster;

import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;

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

    private String signPub;

    private String signCert;

    public IdBroker copy() {
        IdBroker copy = new IdBroker();
        copy.id = id;
        copy.cluster = cluster;
        copy.workspace = workspace;
        copy.signCert = signCert;
        copy.signPub = signPub;
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

    public void setSignKey(String signKey) {
        this.signKey = new Secret(signKey);
    }

    public String getSignPub() {
        return signPub;
    }

    public void setSignPub(String signPub) {
        this.signPub = signPub;
    }

    public String getSignCert() {
        return signCert;
    }

    public void setSignCert(String signCert) {
        this.signCert = signCert;
    }

    @Override
    public String toString() {
        return "IdBroker{" +
                "id=" + id +
                '}';
    }
}
