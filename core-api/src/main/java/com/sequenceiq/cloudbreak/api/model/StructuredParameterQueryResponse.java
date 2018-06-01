package com.sequenceiq.cloudbreak.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class StructuredParameterQueryResponse implements JsonEntity {

    private String propertyName;

    private String description;

    private String defaultPath;

    private String relatedService;

    private String propertyFile;

    private String protocol;

    private String propertyDisplayName;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StructuredParameterQueryResponse)) {
            return false;
        }
        StructuredParameterQueryResponse that = (StructuredParameterQueryResponse) o;
        return Objects.equals(getPropertyName(), that.getPropertyName())
                && Objects.equals(getDescription(), that.getDescription())
                && Objects.equals(getDefaultPath(), that.getDefaultPath())
                && Objects.equals(getRelatedService(), that.getRelatedService())
                && Objects.equals(getPropertyFile(), that.getPropertyFile())
                && Objects.equals(getProtocol(), that.getProtocol())
                && Objects.equals(getPropertyDisplayName(), that.getPropertyDisplayName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPropertyName(), getDescription(), getDefaultPath(), getRelatedService(), getPropertyFile(), getProtocol(),
                getPropertyDisplayName());
    }

}
