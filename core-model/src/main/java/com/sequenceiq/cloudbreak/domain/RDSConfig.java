package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.api.model.RDSDatabase;

@Entity
public class RDSConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "rdsconfig_generator")
    @SequenceGenerator(name = "rdsconfig_generator", sequenceName = "rdsconfig_id_seq", allocationSize = 1)
    private Long id;

    private String connectionURL;
    @Enumerated(EnumType.STRING)
    private RDSDatabase databaseType;
    private String connectionUserName;
    private String connectionPassword;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
}