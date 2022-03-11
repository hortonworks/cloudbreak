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
import com.sequenceiq.cloudbreak.util.DatabaseUtil;

@Entity
@Table(
        uniqueConstraints = {@UniqueConstraint(columnNames = {"name", "account"}), @UniqueConstraint(columnNames = "crn")}
)
public class CustomConfigurations implements Serializable {
    @Id
    @SequenceGenerator(
            name = "custom_configs_generator",
            sequenceName = "custom_configurations_id_seq",
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
    private String crn;

    @OneToMany(mappedBy = "customConfigurations", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<CustomConfigurationProperty> configurations;

    @Column
    private String runtimeVersion;

    @Column
    private String account;

    @Column
    private Long created = System.currentTimeMillis();

    public CustomConfigurations(String name, String crn, Set<CustomConfigurationProperty> configurations, String runtimeVersion, String account, Long created) {
        this.name = name;
        this.crn = crn;
        this.configurations = configurations;
        this.runtimeVersion = runtimeVersion;
        this.account = account;
        this.created = created;
    }

    public CustomConfigurations(CustomConfigurations existingCustomConfigurations) {
        this.name = existingCustomConfigurations.getName();
        this.configurations = Set.copyOf(existingCustomConfigurations.getConfigurations());
        this.runtimeVersion = existingCustomConfigurations.getRuntimeVersion();
    }

    public CustomConfigurations() {
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

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    public Set<CustomConfigurationProperty> getConfigurations() {
        return configurations;
    }

    public void setConfigurations(Set<CustomConfigurationProperty> configurations) {
        this.configurations = configurations;
    }

    public String getRuntimeVersion() {
        return runtimeVersion;
    }

    public void setRuntimeVersion(String runtimeVersion) {
        this.runtimeVersion = runtimeVersion;
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

    @Override
    public String toString() {
        return "CustomConfigs{" +
                "name='" + name + '\'' +
                ", crn='" + crn + '\'' +
                ", configurations='" + DatabaseUtil.lazyLoadSafeToString(configurations) + '\'' +
                ", runtimeVersion='" + runtimeVersion + '\'' +
                ", created=" + created +
                '}';
    }
}
