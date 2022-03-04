package com.sequenceiq.freeipa.entity;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.service.secret.SecretValue;
import com.sequenceiq.cloudbreak.common.dal.model.AccountIdAwareResource;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretToString;
import com.sequenceiq.freeipa.api.model.ResourceStatus;
import com.sequenceiq.freeipa.entity.util.ResourceStatusConverter;

import java.util.StringJoiner;

@Entity
public class Template implements AccountIdAwareResource {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "template_generator")
    @SequenceGenerator(name = "template_generator", sequenceName = "template_id_seq", allocationSize = 1)
    private Long id;

    private String name;

    @Column(length = 1000000, columnDefinition = "TEXT")
    private String description;

    private String instanceType;

    private Integer volumeCount;

    private Integer volumeSize;

    private Integer rootVolumeSize;

    private String volumeType;

    private boolean deleted;

    @Convert(converter = ResourceStatusConverter.class)
    private ResourceStatus status;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json attributes;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret secretAttributes = Secret.EMPTY;

    private String accountId;

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

    public Integer getRootVolumeSize() {
        return rootVolumeSize;
    }

    public void setRootVolumeSize(Integer rootVolumeSize) {
        this.rootVolumeSize = rootVolumeSize;
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

    public Json getAttributes() {
        return attributes;
    }

    public void setAttributes(Json attributes) {
        this.attributes = attributes;
    }

    public String getSecretAttributes() {
        return secretAttributes.getRaw();
    }

    public void setSecretAttributes(String secretAttributes) {
        this.secretAttributes = new Secret(secretAttributes);
    }

    @Override
    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Template.class.getSimpleName() + "[", "]")
                .add("id=" + id)
                .add("name='" + name + '\'')
                .add("description='" + description + "'")
                .add("instanceType='" + instanceType + "'")
                .add("volumeCount=" + volumeCount)
                .add("volumeSize=" + volumeSize)
                .add("rootVolumeSize=" + rootVolumeSize)
                .add("volumeType='" + volumeType + "'")
                .add("deleted=" + deleted)
                .add("status='" + status + "'")
                .add("attributes='" + attributes + "'")
                .add("secretAttributes='***'")
                .add("accountId=" + accountId)
                .toString();
    }
}
