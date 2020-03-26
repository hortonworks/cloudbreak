package com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@NotNull
public class ImageV4Response implements JsonEntity {

    @JsonProperty("date")
    private String date;

    @JsonProperty("created")
    private Long created;

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

    @JsonProperty("cmBuildNumber")
    private String cmBuildNumber;

    @JsonProperty("repository")
    private Map<String, String> repository;

    @JsonProperty("images")
    private Map<String, Map<String, String>> imageSetsByProvider;

    @JsonProperty("stackDetails")
    @JsonInclude(NON_EMPTY)
    private BaseStackDetailsV4Response stackDetails;

    @JsonProperty("preWarmParcels")
    @JsonInclude(NON_EMPTY)
    private List<List<String>> preWarmParcels;

    @JsonProperty("preWarmCsd")
    @JsonInclude(NON_EMPTY)
    private List<String> preWarmCsd;

    @JsonProperty("defaultImage")
    private boolean defaultImage;

    @JsonProperty("packageVersions")
    private Map<String, String> packageVersions;

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

    public BaseStackDetailsV4Response getStackDetails() {
        return stackDetails;
    }

    public void setStackDetails(BaseStackDetailsV4Response stackDetails) {
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

    public Map<String, String> getRepository() {
        return repository;
    }

    public void setRepository(Map<String, String> repository) {
        this.repository = repository;
    }

    public Map<String, Map<String, String>> getImageSetsByProvider() {
        return imageSetsByProvider;
    }

    public void setImageSetsByProvider(Map<String, Map<String, String>> imageSetsByProvider) {
        this.imageSetsByProvider = imageSetsByProvider;
    }

    public Map<String, String> getPackageVersions() {
        return packageVersions;
    }

    public void setPackageVersions(Map<String, String> packageVersions) {
        this.packageVersions = packageVersions;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public String getCmBuildNumber() {
        return cmBuildNumber;
    }

    public void setCmBuildNumber(String cmBuildNumber) {
        this.cmBuildNumber = cmBuildNumber;
    }

    public List<List<String>> getPreWarmParcels() {
        return preWarmParcels;
    }

    public void setPreWarmParcels(List<List<String>> preWarmParcels) {
        this.preWarmParcels = preWarmParcels;
    }

    public List<String> getPreWarmCsd() {
        return preWarmCsd;
    }

    public void setPreWarmCsd(List<String> preWarmCsd) {
        this.preWarmCsd = preWarmCsd;
    }
}
