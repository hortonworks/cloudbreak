package com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.responses;

import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class FileSystemParameterV4Response implements JsonEntity {

    private String propertyName;

    private String description;

    private String defaultPath;

    private Set<String> relatedServices;

    private String propertyFile;

    private String protocol;

    private String propertyDisplayName;

    private boolean secure;

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDefaultPath() {
        return defaultPath;
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

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FileSystemParameterV4Response)) {
            return false;
        }
        FileSystemParameterV4Response that = (FileSystemParameterV4Response) o;
        return secure == that.secure
                && Objects.equals(propertyName, that.propertyName)
                && Objects.equals(description, that.description)
                && Objects.equals(defaultPath, that.defaultPath)
                && Objects.equals(relatedServices, that.relatedServices)
                && Objects.equals(propertyFile, that.propertyFile)
                && Objects.equals(protocol, that.protocol)
                && Objects.equals(propertyDisplayName, that.propertyDisplayName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(propertyName, description, defaultPath, relatedServices, propertyFile, protocol,
                propertyDisplayName, secure);
    }
}
