package com.sequenceiq.environment.user;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name = "user_preferences")
public class UserPreferences implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "userpreferences_generator")
    @SequenceGenerator(name = "userpreferences_generator", sequenceName = "userpreferences_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "externalid")
    private String externalId;

    @Column(name = "auditexternalid")
    private String auditExternalId;

    @Column(name = "user_crn")
    private String userCrn;

    public UserPreferences() {
    }

    public UserPreferences(String externalId, String auditExternalId, String userCrn) {
        this.externalId = externalId;
        this.userCrn = userCrn;
        this.auditExternalId = auditExternalId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getUserCrn() {
        return userCrn;
    }

    public void setUserCrn(String userCrn) {
        this.userCrn = userCrn;
    }

    public String getAuditExternalId() {
        return auditExternalId;
    }

    public void setAuditExternalId(String auditExternalId) {
        this.auditExternalId = auditExternalId;
    }
}
