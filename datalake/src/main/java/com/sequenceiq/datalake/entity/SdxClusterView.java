package com.sequenceiq.datalake.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "sdxcluster")
public class SdxClusterView {

    @Id
    private Long id;

    private String clusterName;

    private String accountId;

    private String crn;

    private String envCrn;

    private String envName;

    public void setId(Long id) {
        this.id = id;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    public void setEnvCrn(String envCrn) {
        this.envCrn = envCrn;
    }

    public void setEnvName(String envName) {
        this.envName = envName;
    }

    public Long getId() {
        return id;
    }

    public String getClusterName() {
        return clusterName;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getCrn() {
        return crn;
    }

    public String getEnvCrn() {
        return envCrn;
    }

    public String getEnvName() {
        return envName;
    }
}
