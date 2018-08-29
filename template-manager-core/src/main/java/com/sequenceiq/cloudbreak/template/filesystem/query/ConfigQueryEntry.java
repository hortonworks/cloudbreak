package com.sequenceiq.cloudbreak.template.filesystem.query;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfigQueryEntry {

    private String propertyName;

    private String description;

    private String defaultPath;

    private String relatedService;

    private String propertyFile;

    private String protocol;

    private String propertyDisplayName;

    private boolean requiredForAttachedCluster;

    private Set<String> supportedStorages = new HashSet<>();

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

    public String getRelatedService() {
        return relatedService;
    }

    public void setRelatedService(String relatedService) {
        this.relatedService = relatedService;
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

    public ConfigQueryEntry copy() {
        ConfigQueryEntry configQueryEntry = new ConfigQueryEntry();
        configQueryEntry.protocol = protocol;
        configQueryEntry.defaultPath = defaultPath;
        configQueryEntry.propertyFile = propertyFile;
        configQueryEntry.description = description;
        configQueryEntry.propertyDisplayName = propertyDisplayName;
        configQueryEntry.relatedService = relatedService;
        configQueryEntry.propertyName = propertyName;
        configQueryEntry.requiredForAttachedCluster = requiredForAttachedCluster;
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
                .append(relatedService)
                .append(propertyFile)
                .append(protocol)
                .append(propertyDisplayName)
                .append(supportedStorages)
                .toHashCode();
    }
}
