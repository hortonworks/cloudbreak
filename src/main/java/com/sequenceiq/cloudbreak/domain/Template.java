package com.sequenceiq.cloudbreak.domain;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = { "azureTemplate_azureTemplateOwner", "name" }),
        @UniqueConstraint(columnNames = { "awsTemplate_awsTemplateOwner", "name" }),
})
public abstract class Template {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "template_generator")
    @SequenceGenerator(name = "template_generator", sequenceName = "sequence_table")
    private Long id;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    private List<UserRole> userRoles = new ArrayList<>();

    private Integer volumeCount;
    private Integer volumeSize;

    public Template() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public abstract void setUser(User user);

    public abstract CloudPlatform cloudPlatform();

    public abstract User getOwner();

    public List<UserRole> getUserRoles() {
        return userRoles;
    }

    public void setUserRoles(List<UserRole> userRoles) {
        this.userRoles = userRoles;
    }

    public Integer getVolumeCount() {
        return volumeCount;
    }

    public void setVolumeCount(Integer volumeCount) {
        this.volumeCount = volumeCount;
    }

    public Integer getVolumeSize() {
        return volumeSize;
    }

    public void setVolumeSize(Integer volumeSize) {
        this.volumeSize = volumeSize;
    }

}
