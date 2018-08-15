package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.sequenceiq.cloudbreak.authorization.OrganizationResource;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.OrganizationAwareResource;

@Entity
@Table(name = "ImageCatalog", uniqueConstraints = @UniqueConstraint(columnNames = {"organization_id", "name"}))
public class ImageCatalog implements ProvisionEntity, OrganizationAwareResource {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "imagecatalog_generator")
    @SequenceGenerator(name = "imagecatalog_generator", sequenceName = "imagecatalog_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String owner;

    @Column(nullable = false)
    private String account;

    @Column(nullable = false)
    private String name;

    @Column(name = "url", nullable = false)
    private String imageCatalogUrl;

    @Column(nullable = false)
    private boolean publicInAccount;

    @Column(columnDefinition = "boolean default false")
    private boolean archived;

    @ManyToOne
    private Organization organization;

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

    public void setName(String name) {
        this.name = name;
    }

    public String getImageCatalogUrl() {
        return imageCatalogUrl;
    }

    public void setImageCatalogUrl(String imageCatalogUrl) {
        this.imageCatalogUrl = imageCatalogUrl;
    }

    public boolean isPublicInAccount() {
        return publicInAccount;
    }

    public void setPublicInAccount(boolean publicInAccount) {
        this.publicInAccount = publicInAccount;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public Organization getOrganization() {
        return organization;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    @Override
    public OrganizationResource getResource() {
        return OrganizationResource.IMAGECATALOG;
    }
}
