package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.sequenceiq.cloudbreak.common.type.CloudPlatform;
import com.sequenceiq.cloudbreak.common.type.ResourceStatus;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.json.JsonToString;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = { "account", "name" }),
})
@NamedQueries({
        @NamedQuery(
                name = "Template.findForUser",
                query = "SELECT t FROM Template t "
                        + "WHERE t.owner= :user AND deleted IS NOT TRUE "
                        + "AND t.status <> 'DEFAULT_DELETED' "),
        @NamedQuery(
                name = "Template.findPublicInAccountForUser",
                query = "SELECT t FROM Template t "
                        + "WHERE ((t.account= :account AND t.publicInAccount= true) "
                        + "OR t.owner= :user) AND deleted IS NOT TRUE "
                        + "AND t.status <> 'DEFAULT_DELETED' "),
        @NamedQuery(
                name = "Template.findAllInAccount",
                query = "SELECT t FROM Template t "
                        + "WHERE t.account= :account AND deleted IS NOT TRUE "
                        + "AND t.status <> 'DEFAULT_DELETED' "),
        @NamedQuery(
                name = "Template.findOneByName",
                query = "SELECT t FROM Template t "
                        + "WHERE t.name= :name and t.account= :account AND deleted IS NOT TRUE "
                        + "AND t.status <> 'DEFAULT_DELETED' "),
        @NamedQuery(
                name = "Template.findByIdInAccount",
                query = "SELECT t FROM Template t "
                        + "WHERE t.id= :id and t.account= :account AND deleted IS NOT TRUE "
                        + "AND t.status <> 'DEFAULT_DELETED' "),
        @NamedQuery(
                name = "Template.findByNameInAccount",
                query = "SELECT t FROM Template t "
                        + "WHERE t.name= :name and ((t.account= :account and t.publicInAccount=true) or t.owner= :owner) "
                        + "AND deleted IS NOT TRUE "
                        + "AND t.status <> 'DEFAULT_DELETED' "),
        @NamedQuery(
                name = "Template.findByNameInUser",
                query = "SELECT t FROM Template t "
                        + "WHERE t.owner= :owner and t.name= :name AND deleted IS NOT TRUE "
                        + "AND t.status <> 'DEFAULT_DELETED' "),
        @NamedQuery(
                name = "Template.findAllDefaultInAccount",
                query = "SELECT t FROM Template t "
                        + "WHERE t.account= :account "
                        + "AND (t.status = 'DEFAULT_DELETED' OR t.status = 'DEFAULT') ")
})
public class Template {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "template_generator")
    @SequenceGenerator(name = "template_generator", sequenceName = "template_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String name;
    @Column(length = 1000000, columnDefinition = "TEXT")
    private String description;

    private String instanceType;

    private String owner;
    private String account;

    private boolean publicInAccount;

    private Integer volumeCount;
    private Integer volumeSize;
    private String volumeType;

    private boolean deleted;

    @Enumerated(EnumType.STRING)
    private ResourceStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CloudPlatform cloudPlatform;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json attributes;

    public Template() {
        deleted = false;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVolumeType() {
        return volumeType;
    }

    public void setVolumeType(String volumeType) {
        this.volumeType = volumeType;
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

    public boolean isPublicInAccount() {
        return publicInAccount;
    }

    public void setPublicInAccount(boolean publicInAccount) {
        this.publicInAccount = publicInAccount;
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

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public ResourceStatus getStatus() {
        return status;
    }

    public void setStatus(ResourceStatus status) {
        this.status = status;
    }

    public String getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }

    public CloudPlatform cloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(CloudPlatform cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public Json getAttributes() {
        return attributes;
    }

    public void setAttributes(Json attributes) {
        this.attributes = attributes;
    }
}
