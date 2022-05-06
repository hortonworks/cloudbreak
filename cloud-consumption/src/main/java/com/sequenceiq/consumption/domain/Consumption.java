package com.sequenceiq.consumption.domain;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.sequenceiq.cloudbreak.auth.security.AuthResource;
import com.sequenceiq.cloudbreak.common.dal.model.AccountAwareResource;
import com.sequenceiq.consumption.api.v1.consumption.model.common.ConsumptionType;
import com.sequenceiq.consumption.api.v1.consumption.model.common.ResourceType;
import com.sequenceiq.consumption.converter.ConsumptionTypeConverter;
import com.sequenceiq.consumption.converter.ResourceTypeConverter;

@Entity
@Table
public class Consumption implements AuthResource, AccountAwareResource {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "consumption_generator")
    @SequenceGenerator(name = "consumption_generator", sequenceName = "consumption_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000000, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String accountId;

    @Column(nullable = false)
    private String resourceCrn;

    @Column(nullable = false)
    private String environmentCrn;

    @Column(nullable = false)
    @Convert(converter = ResourceTypeConverter.class)
    private ResourceType monitoredResourceType;

    @Column(nullable = false)
    private String monitoredResourceCrn;

    @Column(nullable = false)
    @Convert(converter = ConsumptionTypeConverter.class)
    private ConsumptionType consumptionType;

    private String storageLocation;

    public Consumption() {

    }

    @Override
    public String getAccountId() {
        return accountId;
    }

    @Override
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    @Override
    public String getResourceName() {
        return name;
    }

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getResourceCrn() {
        return resourceCrn;
    }

    @Override
    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public ResourceType getMonitoredResourceType() {
        return monitoredResourceType;
    }

    public void setMonitoredResourceType(ResourceType monitoredResourceType) {
        this.monitoredResourceType = monitoredResourceType;
    }

    public String getMonitoredResourceCrn() {
        return monitoredResourceCrn;
    }

    public void setMonitoredResourceCrn(String monitoredResourceCrn) {
        this.monitoredResourceCrn = monitoredResourceCrn;
    }

    public ConsumptionType getConsumptionType() {
        return consumptionType;
    }

    public void setConsumptionType(ConsumptionType consumptionType) {
        this.consumptionType = consumptionType;
    }

    public String getStorageLocation() {
        return storageLocation;
    }

    public void setStorageLocation(String storageLocation) {
        this.storageLocation = storageLocation;
    }

    @Override
    public String toString() {
        return "Consumption{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", accountId='" + accountId + '\'' +
                ", resourceCrn='" + resourceCrn + '\'' +
                ", environmentCrn='" + environmentCrn + '\'' +
                ", monitoredResourceType='" + monitoredResourceType.name() + '\'' +
                ", monitoredResourceCrn='" + monitoredResourceCrn + '\'' +
                ", consumptionType='" + consumptionType.name() + '\'' +
                ", storageLocation='" + storageLocation + '\'' +
                '}';
    }
}
