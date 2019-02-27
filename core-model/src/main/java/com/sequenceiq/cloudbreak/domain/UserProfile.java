package com.sequenceiq.cloudbreak.domain;

import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
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

import com.sequenceiq.cloudbreak.aspect.secret.SecretValue;
import com.sequenceiq.cloudbreak.domain.workspace.Tenant;
import com.sequenceiq.cloudbreak.domain.workspace.TenantAwareResource;
import com.sequenceiq.cloudbreak.domain.workspace.User;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "user_id"))
public class UserProfile implements TenantAwareResource {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "userprofile_generator")
    @SequenceGenerator(name = "userprofile_generator", sequenceName = "userprofile_id_seq", allocationSize = 1)
    private Long id;

    @OneToOne
    private ImageCatalog imageCatalog;

    @Column
    private String userName;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret uiProperties = Secret.EMPTY;

    @OneToOne
    private User user;

    @ManyToMany(fetch = FetchType.EAGER)
    private Set<Credential> defaultCredentials;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private ShowTerminatedClustersPreferences showTerminatedClustersPreferences;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUiProperties() {
        return uiProperties.getRaw();
    }

    public String getUiPropertiesSecret() {
        return uiProperties.getSecret();
    }

    public void setUiProperties(String uiProperties) {
        this.uiProperties = new Secret(uiProperties);
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

    public ShowTerminatedClustersPreferences getShowTerminatedClustersPreferences() {
        return showTerminatedClustersPreferences;
    }

    public void setShowTerminatedClustersPreferences(ShowTerminatedClustersPreferences showTerminatedClustersPreferences) {
        this.showTerminatedClustersPreferences = showTerminatedClustersPreferences;
    }

    @Override
    public Tenant getTenant() {
        return user.getTenant();
    }
}
