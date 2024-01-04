package com.sequenceiq.cloudbreak.workspace.model;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_preferences")
public class UserPreferences implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "userpreferences_generator")
    @SequenceGenerator(name = "userpreferences_generator", sequenceName = "userpreferences_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "externalid")
    private String externalId;

    @OneToOne
    private User user;

    public UserPreferences() {
    }

    public UserPreferences(String externalId, User user) {
        this.externalId = externalId;
        this.user = user;
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
