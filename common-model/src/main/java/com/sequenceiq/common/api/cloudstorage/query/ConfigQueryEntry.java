package com.sequenceiq.common.api.cloudstorage.query;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.model.CloudStorageCdpService;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfigQueryEntry {

    private CloudStorageCdpService type;

    private String propertyName;

    private String description;

    private String defaultPath;

    private Set<String> relatedServices = new HashSet<>();

    private String propertyFile;

    private String protocol;

    private String propertyDisplayName;

    private boolean requiredForAttachedCluster;

    private boolean secure;

    private Set<String> relatedMissingServices = new HashSet<>();

    private Set<String> supportedStorages = new HashSet<>();

    public CloudStorageCdpService getType() {
        return type;
    }

    public void setType(CloudStorageCdpService type) {
        this.type = type;
    }

    public Set<String> getSupportedStorages() {
        return supportedStorages;
    }

    public void setSupportedStorages(Set<String> supportedStorages) {
        this.supportedStorages = supportedStorages;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public String getDescription() {
        return description;
    }

    public String getDefaultPath() {
        return defaultPath;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDefaultPath(String defaultPath) {
        this.defaultPath = defaultPath;
    }

    public Set<String> getRelatedServices() {
        return relatedServices;
    }

    public void setRelatedServices(Set<String> relatedServices) {
        this.relatedServices = relatedServices;
    }

    public String getPropertyFile() {
        return propertyFile;
    }

    public void setPropertyFile(String propertyFile) {
        this.propertyFile = propertyFile;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getPropertyDisplayName() {
        return propertyDisplayName;
    }

    public void setPropertyDisplayName(String propertyDisplayName) {
        this.propertyDisplayName = propertyDisplayName;
    }

    public boolean isRequiredForAttachedCluster() {
        return requiredForAttachedCluster;
    }

    public void setRequiredForAttachedCluster(boolean requiredForAttachedCluster) {
        this.requiredForAttachedCluster = requiredForAttachedCluster;
    }

    public Set<String> getRelatedMissingServices() {
        return relatedMissingServices;
    }

    public void setRelatedMissingServices(Set<String> relatedMissingServices) {
        this.relatedMissingServices = relatedMissingServices;
    }

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public ConfigQueryEntry copy() {
        ConfigQueryEntry configQueryEntry = new ConfigQueryEntry();
        configQueryEntry.type = type;
        configQueryEntry.protocol = protocol;
        configQueryEntry.defaultPath = defaultPath;
        configQueryEntry.propertyFile = propertyFile;
        configQueryEntry.description = description;
        configQueryEntry.propertyDisplayName = propertyDisplayName;
        configQueryEntry.relatedServices = relatedServices;
        configQueryEntry.propertyName = propertyName;
        configQueryEntry.requiredForAttachedCluster = requiredForAttachedCluster;
        configQueryEntry.secure = secure;
        configQueryEntry.supportedStorages = new HashSet<>(supportedStorages);
        configQueryEntry.relatedMissingServices = relatedMissingServices;
        return configQueryEntry;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !Objects.equals(getClass(), o.getClass())) {
            return false;
        } else if (this == o) {
            return true;
        }

        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(propertyName)
                .append(description)
                .append(defaultPath)
                .append(relatedServices)
                .append(relatedMissingServices)
                .append(propertyFile)
                .append(protocol)
                .append(propertyDisplayName)
                .append(supportedStorages)
                .append(secure)
                .toHashCode();
    }
}
