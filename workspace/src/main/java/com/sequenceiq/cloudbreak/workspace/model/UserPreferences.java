package com.sequenceiq.cloudbreak.workspace.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
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

    @Override
    public String toString() {
        return "UserPreferences{" +
                "id=" + id +
                ", externalId='" + externalId + '\'' +
                ", user=" + user +
                '}';
    }
}
