package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.sequenceiq.cloudbreak.api.model.ResourceStatus;
import com.sequenceiq.cloudbreak.domain.converter.EncryptionConverter;
import com.sequenceiq.cloudbreak.domain.json.EncryptedJsonToString;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.json.JsonToString;
import com.sequenceiq.cloudbreak.domain.security.Organization;

@Entity
@Table(name = "Blueprint", uniqueConstraints = @UniqueConstraint(columnNames = {"account", "name"}))
public class Blueprint implements ProvisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "blueprint_generator")
    @SequenceGenerator(name = "blueprint_generator", sequenceName = "blueprint_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Convert(converter = EncryptionConverter.class)
    @Column(length = 1000000, columnDefinition = "TEXT", nullable = false)
    private String blueprintText;

    private String ambariName;

    @Column(length = 1000000, columnDefinition = "TEXT")
    private String description;

    private int hostGroupCount;

    @Column(nullable = false)
    private String owner;

    @Column(nullable = false)
    private String account;

    @Column(nullable = false)
    private boolean publicInAccount;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ResourceStatus status;

    @Convert(converter = EncryptedJsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json inputParameters;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json tags;

    @ManyToOne
    private Organization organization;

    public Json getTags() {
        return tags;
    }

    public void setTags(Json tags) {
        this.tags = tags;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBlueprintText() {
        return blueprintText;
    }

    public void setBlueprintText(String blueprintText) {
        this.blueprintText = blueprintText;
    }

    public String getAmbariName() {
        return ambariName;
    }

    public void setAmbariName(String ambariName) {
        this.ambariName = ambariName;
    }

    public int getHostGroupCount() {
        return hostGroupCount;
    }

    public void setHostGroupCount(int hostGroupCount) {
        this.hostGroupCount = hostGroupCount;
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

    public ResourceStatus getStatus() {
        return status;
    }

    public void setStatus(ResourceStatus status) {
        this.status = status;
    }

    public Json getInputParameters() {
        return inputParameters;
    }

    public void setInputParameters(Json inputParameters) {
        this.inputParameters = inputParameters;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }
}
