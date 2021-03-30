package com.sequenceiq.cloudbreak.domain;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "customconfigs_properties")
public class CustomConfigProperty implements Serializable {

    @Id
    @SequenceGenerator(name = "custom_config_property_generator", sequenceName = "custom_config_property_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "custom_config_property_generator")
    private Long id;

    @Column(nullable = false)
    private String configName;

    @Column(nullable = false)
    private String configValue;

    @Column
    private String roleType;

    @Column(nullable = false)
    private String serviceType;

    @ManyToOne
    private CustomConfigs customConfigs;

    public CustomConfigProperty(String configName, String configValue, String roleType, String serviceType) {
        this.configName = configName;
        this.configValue = configValue;
        this.roleType = roleType;
        this.serviceType = serviceType;
    }

    public CustomConfigProperty() {
    }

    @JsonIgnore
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }

    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }

    public String getRoleType() {
        return roleType;
    }

    public void setRoleType(String roleType) {
        this.roleType = roleType;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public CustomConfigs getCustomConfigs() {
        return customConfigs;
    }

    public void setCustomConfigs(CustomConfigs customConfigs) {
        this.customConfigs = customConfigs;
    }

    @Override
    public String toString() {
        return "CustomConfigProperty{" +
                "id=" + id +
                ", configName='" + configName + '\'' +
                ", configValue='" + configValue + '\'' +
                ", roleType='" + roleType + '\'' +
                ", serviceType='" + serviceType + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CustomConfigProperty)) {
            return false;
        }
        CustomConfigProperty property = (CustomConfigProperty) o;
        return getConfigName().equals(property.getConfigName()) && getConfigValue().equals(property.getConfigValue()) &&
                Objects.equals(getRoleType(), property.getRoleType()) && getServiceType().equals(property.getServiceType());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getConfigName(), getConfigValue(), getRoleType(), getServiceType());
    }
}
