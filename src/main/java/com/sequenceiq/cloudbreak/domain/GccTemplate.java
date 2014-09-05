package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccImageType;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccInstanceType;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccZone;

@Entity
public class GccTemplate extends Template implements ProvisionEntity {

    @Column(nullable = false)
    private String name;
    private String description;
    @Enumerated(EnumType.STRING)
    private GccZone gccZone;
    @Enumerated(EnumType.STRING)
    private GccImageType gccImageType;
    @Enumerated(EnumType.STRING)
    private GccInstanceType gccInstanceType;
    @ManyToOne
    @JoinColumn(name = "gccTemplate_gccTemplateOwner")
    private User gccTemplateOwner;

    public GccTemplate() {

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public GccZone getGccZone() {
        return gccZone;
    }

    public void setGccZone(GccZone gccZone) {
        this.gccZone = gccZone;
    }

    public GccImageType getGccImageType() {
        return gccImageType;
    }

    public void setGccImageType(GccImageType gccImageType) {
        this.gccImageType = gccImageType;
    }

    public GccInstanceType getGccInstanceType() {
        return gccInstanceType;
    }

    public void setGccInstanceType(GccInstanceType gccInstanceType) {
        this.gccInstanceType = gccInstanceType;
    }

    public User getGccTemplateOwner() {
        return gccTemplateOwner;
    }

    public void setGccTemplateOwner(User gccTemplateOwner) {
        this.gccTemplateOwner = gccTemplateOwner;
    }

    @Override
    public void setUser(User user) {
        this.gccTemplateOwner = user;
    }

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.GCC;
    }

    @Override
    public User getOwner() {
        return gccTemplateOwner;
    }
}
