package com.sequenceiq.cloudbreak.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.sequenceiq.cloudbreak.service.secret.SecretValue;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretToString;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.TenantAwareResource;
import com.sequenceiq.cloudbreak.workspace.model.User;

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
