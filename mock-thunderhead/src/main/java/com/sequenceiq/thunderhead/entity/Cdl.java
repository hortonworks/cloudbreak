package com.sequenceiq.thunderhead.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Cdl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String crn;

    @Column(nullable = false)
    private String environmentCrn;

    private String databaseServerCrn;

    private String hmsDatabaseHost;

    private String hmsDatabaseUser;

    private String hmsDatabasePassword;

    private String hmsDatabaseName;

    private String rangerFqdn;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public String getDatabaseServerCrn() {
        return databaseServerCrn;
    }

    public void setDatabaseServerCrn(String databaseServerCrn) {
        this.databaseServerCrn = databaseServerCrn;
    }

    public String getHmsDatabaseHost() {
        return hmsDatabaseHost;
    }

    public void setHmsDatabaseHost(String hmsDatabaseHost) {
        this.hmsDatabaseHost = hmsDatabaseHost;
    }

    public String getHmsDatabaseUser() {
        return hmsDatabaseUser;
    }

    public void setHmsDatabaseUser(String hmsDatabaseUser) {
        this.hmsDatabaseUser = hmsDatabaseUser;
    }

    public String getHmsDatabasePassword() {
        return hmsDatabasePassword;
    }

    public void setHmsDatabasePassword(String hmsDatabasePassword) {
        this.hmsDatabasePassword = hmsDatabasePassword;
    }

    public String getHmsDatabaseName() {
        return hmsDatabaseName;
    }

    public void setHmsDatabaseName(String hmsDatabaseName) {
        this.hmsDatabaseName = hmsDatabaseName;
    }

    public String getRangerFqdn() {
        return rangerFqdn;
    }

    public void setRangerFqdn(String rangerFqdn) {
        this.rangerFqdn = rangerFqdn;
    }
}
