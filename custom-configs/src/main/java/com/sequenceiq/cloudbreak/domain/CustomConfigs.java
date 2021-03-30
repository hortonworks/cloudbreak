package com.sequenceiq.cloudbreak.domain;

import java.io.Serializable;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(
        uniqueConstraints = @UniqueConstraint(columnNames = {"name", "resourcecrn"})
)
public class CustomConfigs implements Serializable {
    @Id
    @SequenceGenerator(
            name = "custom_configs_generator",
            sequenceName = "custom_configs_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "custom_configs_generator"
    )
    @Column
    private Long id;

    @Column
    private String name;

    @Column
    private String resourceCrn;

    @OneToMany(mappedBy = "customConfigs", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<CustomConfigProperty> configs;

    @Column
    private String platformVersion;

    @Column
    private String account;

    @Column
    private Long created = System.currentTimeMillis();

    @Column
    private Long lastModified;

    public CustomConfigs(String name, String resourceCrn, Set<CustomConfigProperty> configs, String platformVersion,
            Long created, Long lastModified) {
        this.name = name;
        this.resourceCrn = resourceCrn;
        this.configs = configs;
        this.platformVersion = platformVersion;
        this.created = created;
        this.lastModified = lastModified;
    }

    public CustomConfigs(CustomConfigs existingCustomConfigs) {
        this.name = existingCustomConfigs.name;
        this.configs = existingCustomConfigs.configs;
        this.platformVersion = existingCustomConfigs.platformVersion;
    }

    public CustomConfigs() {
    }

    @JsonIgnore
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Set<CustomConfigProperty> getConfigs() {
        return configs;
    }

    public void setConfigs(Set<CustomConfigProperty> configs) {
        this.configs = configs;
    }

    public String getPlatformVersion() {
        return platformVersion;
    }

    public void setPlatformVersion(String platformVersion) {
        this.platformVersion = platformVersion;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public Long getLastModified() {
        return lastModified;
    }

    public void setLastModified(Long lastModified) {
        this.lastModified = lastModified;
    }

    @Override
    public String toString() {
        return "CustomConfigs{" +
                "customConfigsName='" + name + '\'' +
                ", resourceCrn='" + resourceCrn + '\'' +
                ", configs='" + configs + '\'' +
                ", platformVersion='" + platformVersion + '\'' +
                ", created=" + created +
                ", lastModified=" + lastModified +
                '}';
    }
}
