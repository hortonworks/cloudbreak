package com.sequenceiq.cloudbreak.domain;

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.sequenceiq.cloudbreak.aspect.vault.VaultValue;
import com.sequenceiq.cloudbreak.domain.workspace.User;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"account", "owner"}))
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "userprofile_generator")
    @SequenceGenerator(name = "userprofile_generator", sequenceName = "userprofile_id_seq", allocationSize = 1)
    private Long id;

    @OneToOne
    private ImageCatalog imageCatalog;

    @Column(nullable = false)
    private String owner;

    @Column(nullable = false)
    private String account;

    @Column
    private String userName;

    @VaultValue
    @Column(columnDefinition = "TEXT")
    private String uiProperties;

    @OneToOne
    private User user;

    @ManyToMany(fetch = FetchType.EAGER)
    private Set<Credential> defaultCredentials;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUiProperties() {
        return uiProperties;
    }

    public void setUiProperties(String uiProperties) {
        this.uiProperties = uiProperties;
    }

    public ImageCatalog getImageCatalog() {
        return imageCatalog;
    }

    public void setImageCatalog(ImageCatalog imageCatalog) {
        this.imageCatalog = imageCatalog;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Set<Credential> getDefaultCredentials() {
        return defaultCredentials;
    }

    public void setDefaultCredentials(Set<Credential> defaultCredentials) {
        this.defaultCredentials = defaultCredentials;
    }
}
