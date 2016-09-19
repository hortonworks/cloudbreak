package com.sequenceiq.cloudbreak.domain;

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.sequenceiq.cloudbreak.api.model.RDSDatabase;
import com.sequenceiq.cloudbreak.common.type.RdsType;
import com.sequenceiq.cloudbreak.common.type.ResourceStatus;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.json.JsonToString;

@Entity
@Table(name = "RDSConfig", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "account", "name" })
})
@NamedQueries({
        @NamedQuery(
                name = "RDSConfig.findForUser",
                query = "SELECT r FROM RDSConfig r "
                        + "LEFT JOIN FETCH r.clusters "
                        + "WHERE r.owner= :user "),
        @NamedQuery(
                name = "RDSConfig.findPublicInAccountForUser",
                query = "SELECT r FROM RDSConfig r "
                        + "LEFT JOIN FETCH r.clusters "
                        + "WHERE ((r.account= :account AND r.publicInAccount= true) "
                        + "OR r.owner= :user) "
                        + "AND r.status <> 'DEFAULT_DELETED' "),
        @NamedQuery(
                name = "RDSConfig.findAllInAccount",
                query = "SELECT r FROM RDSConfig r "
                        + "LEFT JOIN FETCH r.clusters "
                        + "WHERE r.account= :account "
                        + "AND r.status <> 'DEFAULT_DELETED' "),
        @NamedQuery(
                name = "RDSConfig.findByNameInUser",
                query = "SELECT r FROM RDSConfig r "
                        + "LEFT JOIN FETCH r.clusters "
                        + "WHERE r.owner= :owner and r.name= :name "
                        + "AND r.status <> 'DEFAULT_DELETED' "),
        @NamedQuery(
                name = "RDSConfig.findOneByName",
                query = "SELECT r FROM RDSConfig r "
                        + "LEFT JOIN FETCH r.clusters "
                        + "WHERE r.name= :name and r.account= :account "
                        + "AND r.status <> 'DEFAULT_DELETED' "),
        @NamedQuery(
                name = "RDSConfig.findByIdInAccount",
                query = "SELECT r FROM RDSConfig r "
                        + "LEFT JOIN FETCH r.clusters "
                        + "WHERE  r.id= :id and r.account= :account "
                        + "AND r.status <> 'DEFAULT_DELETED' "),
        @NamedQuery(
                name = "RDSConfig.findById",
                query = "SELECT r FROM RDSConfig r "
                        + "LEFT JOIN FETCH r.clusters "
                        + "WHERE  r.id= :id "
                        + "AND r.status <> 'DEFAULT_DELETED' "),
        @NamedQuery(
                name = "RDSConfig.findByNameInAccount",
                query = "SELECT r FROM RDSConfig r "
                        + "LEFT JOIN FETCH r.clusters "
                        + "WHERE  r.name= :name and ((r.publicInAccount=true and r.account= :account) or r.owner= :owner) "
                        + "AND r.status <> 'DEFAULT_DELETED' "),
})
public class RDSConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "rdsconfig_generator")
    @SequenceGenerator(name = "rdsconfig_generator", sequenceName = "rdsconfig_id_seq", allocationSize = 1)
    private Long id;

    private String name;

    private String connectionURL;
    @Enumerated(EnumType.STRING)
    private RDSDatabase databaseType;
    private String connectionUserName;
    private String connectionPassword;
    private Long creationDate;
    private String hdpVersion;

    private String owner;
    private String account;

    private boolean publicInAccount;

    @Enumerated(EnumType.STRING)
    private ResourceStatus status;

    @OneToMany(mappedBy = "rdsConfig")
    private Set<Cluster> clusters;

    @Enumerated(EnumType.STRING)
    private RdsType type = RdsType.HIVE;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json attributes;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getConnectionURL() {
        return connectionURL;
    }

    public void setConnectionURL(String connectionURL) {
        this.connectionURL = connectionURL;
    }

    public RDSDatabase getDatabaseType() {
        return databaseType;
    }

    public void setDatabaseType(RDSDatabase databaseType) {
        this.databaseType = databaseType;
    }

    public String getConnectionUserName() {
        return connectionUserName;
    }

    public void setConnectionUserName(String connectionUserName) {
        this.connectionUserName = connectionUserName;
    }

    public String getConnectionPassword() {
        return connectionPassword;
    }

    public void setConnectionPassword(String connectionPassword) {
        this.connectionPassword = connectionPassword;
    }

    public Long getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Long creationDate) {
        this.creationDate = creationDate;
    }

    public String getHdpVersion() {
        return hdpVersion;
    }

    public void setHdpVersion(String hdpVersion) {
        this.hdpVersion = hdpVersion;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public void setPublicInAccount(boolean publicInAccount) {
        this.publicInAccount = publicInAccount;
    }

    public boolean isPublicInAccount() {
        return publicInAccount;
    }

    public ResourceStatus getStatus() {
        return status;
    }

    public void setStatus(ResourceStatus status) {
        this.status = status;
    }

    public Set<Cluster> getClusters() {
        return clusters;
    }

    public void setClusters(Set<Cluster> clusters) {
        this.clusters = clusters;
    }

    public RdsType getType() {
        return type;
    }

    public void setType(RdsType type) {
        this.type = type;
    }

    public Json getAttributes() {
        return attributes;
    }

    public void setAttributes(Json attributes) {
        this.attributes = attributes;
    }
}