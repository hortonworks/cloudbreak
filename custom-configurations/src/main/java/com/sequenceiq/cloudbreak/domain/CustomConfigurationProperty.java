package com.sequenceiq.cloudbreak.domain;

import static com.google.common.base.Strings.isNullOrEmpty;

import java.io.Serializable;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sequenceiq.cloudbreak.service.secret.SecretValue;
import com.sequenceiq.cloudbreak.service.secret.domain.AccountIdAwareResource;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretToString;

@Entity
@Table(name = "customconfigurations_properties")
public class CustomConfigurationProperty implements Serializable, AccountIdAwareResource {

    @Id
    @SequenceGenerator(name = "custom_config_property_generator", sequenceName = "custom_configuration_property_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "custom_config_property_generator")
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String value;

    @Column(nullable = false)
    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret secretValue = Secret.EMPTY;

    @Column
    private String roleType;

    @Column(nullable = false)
    private String serviceType;

    @ManyToOne(cascade = CascadeType.ALL)
    private CustomConfigurations customConfigurations;

    public CustomConfigurationProperty(String name, String value, String roleType, String serviceType) {
        this.name = name;
        this.secretValue = new Secret(value);
        this.roleType = roleType;
        this.serviceType = serviceType;
    }

    public CustomConfigurationProperty() {
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

    public String getSecretValue() {
        return secretValue.getRaw();
    }

    public void setSecretValue(String secretValue) {
        this.secretValue = new Secret(secretValue);
    }

    public String getSecret() {
        return secretValue.getSecret();
    }

    public String getValue() {
        return isNullOrEmpty(value) ? getSecretValue() : value;
    }

    public void setValue(String value) {
        this.value = value;
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

    public CustomConfigurations getCustomConfigs() {
        return customConfigurations;
    }

    public void setCustomConfigs(CustomConfigurations customConfigurations) {
        this.customConfigurations = customConfigurations;
    }

    @Override
    public String getAccountId() {
        return getCustomConfigs().getAccount();
    }

    @Override
    public String toString() {
        return "CustomConfigurationProperty{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", roleType='" + roleType + '\'' +
                ", serviceType='" + serviceType + '\'' +
                '}';
    }
}
