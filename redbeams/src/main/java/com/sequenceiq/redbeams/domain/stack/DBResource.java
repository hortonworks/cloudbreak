package com.sequenceiq.redbeams.domain.stack;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@Entity
@Table(name = "resource")
public class DBResource {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "resource_generator")
    @SequenceGenerator(name = "resource_generator", sequenceName = "resource_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "resource_dbstack")
    private DBStack dbStack;

    @Column(nullable = false)
    private String resourceName;

    @Column(nullable = false)
    private String resourceReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResourceType resourceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommonStatus resourceStatus;

    public Long getId() {
        return id;
    }

    public DBStack getDbStack() {
        return dbStack;
    }

    public String getResourceName() {
        return resourceName;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public CommonStatus getResourceStatus() {
        return resourceStatus;
    }

    public String getResourceReference() {
        return resourceReference;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setDbStack(DBStack dbStack) {
        this.dbStack = dbStack;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public void setResourceStatus(CommonStatus resourceStatus) {
        this.resourceStatus = resourceStatus;
    }

    public void setResourceReference(String resourceReference) {
        this.resourceReference = resourceReference;
    }

    public static class Builder {
        private final DBResource dbResource = new DBResource();

        public Builder withName(String resourceName) {
            dbResource.setResourceName(resourceName);
            return this;
        }

        public Builder withReference(String resourceReference) {
            dbResource.setResourceReference(resourceReference);
            return this;
        }

        public Builder withStatus(CommonStatus resourceStatus) {
            dbResource.setResourceStatus(resourceStatus);
            return this;
        }

        public Builder withType(ResourceType resourceType) {
            dbResource.setResourceType(resourceType);
            return this;
        }

        public DBResource build() {
            return dbResource;
        }
    }
}
