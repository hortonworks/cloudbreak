package com.sequenceiq.environment.environment.domain;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "Environment")
public class ParentEnvironmentView implements Serializable {

    @Id
    private Long id;

    @Column(nullable = false)
    private String cloudPlatform;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String resourceCrn;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }
}
