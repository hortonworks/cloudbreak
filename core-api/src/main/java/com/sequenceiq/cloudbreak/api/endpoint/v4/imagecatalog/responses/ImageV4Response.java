package com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

import java.util.Set;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.api.model.imagecatalog.StackDetailsJson;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@NotNull
public class ImageV4Response implements JsonEntity {

    @JsonProperty("date")
    private String date;

    @JsonProperty("description")
    private String description;

    @JsonProperty("os")
    private String os;

    @JsonProperty("osType")
    private String osType;

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("version")
    private String version;

    @JsonProperty("ambariRepoUrl")
    private String ambariRepoUrl;

    @JsonProperty("locations")
    private Set<ImageLocationV4Response> locations;

    @JsonProperty("stackDetails")
    @JsonInclude(NON_EMPTY)
    private StackDetailsJson stackDetails;

    @JsonProperty("defaultImage")
    private boolean defaultImage;

    @JsonProperty("packageVersions")
    private Set<PackageVersionV4Response> packageVersions;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getAmbariRepoUrl() {
        return ambariRepoUrl;
    }

    public void setAmbariRepoUrl(String ambariRepoUrl) {
        this.ambariRepoUrl = ambariRepoUrl;
    }

    public Set<ImageLocationV4Response> getLocations() {
        return locations;
    }

    public void setLocations(Set<ImageLocationV4Response> locations) {
        this.locations = locations;
    }

    public StackDetailsJson getStackDetails() {
        return stackDetails;
    }

    public void setStackDetails(StackDetailsJson stackDetails) {
        this.stackDetails = stackDetails;
    }

    public String getOsType() {
        return osType;
    }

    public void setOsType(String osType) {
        this.osType = osType;
    }

    public boolean isDefaultImage() {
        return defaultImage;
    }

    public void setDefaultImage(boolean defaultImage) {
        this.defaultImage = defaultImage;
    }

    public Set<PackageVersionV4Response> getPackageVersions() {
        return packageVersions;
    }

    public void setPackageVersions(Set<PackageVersionV4Response> packageVersions) {
        this.packageVersions = packageVersions;
    }
}
