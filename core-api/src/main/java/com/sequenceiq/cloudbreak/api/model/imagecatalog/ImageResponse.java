package com.sequenceiq.cloudbreak.api.model.imagecatalog;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ImageResponse implements JsonEntity {

    @JsonProperty("date")
    private String date;

    @JsonProperty("description")
    private String description;

    @JsonProperty("os")
    private String os;

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("version")
    private String version;

    @JsonProperty("repo")
    private Map<String, String> repo;

    @JsonProperty("images")
    private Map<String, Map<String, String>> imageSetsByProvider;

    @JsonProperty("stackDetails")
    private StackDetailsJson stackDetails;

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

    public Map<String, String> getRepo() {
        return repo;
    }

    public void setRepo(Map<String, String> repo) {
        this.repo = repo;
    }

    public Map<String, Map<String, String>> getImageSetsByProvider() {
        return imageSetsByProvider;
    }

    public void setImageSetsByProvider(Map<String, Map<String, String>> imageSetsByProvider) {
        this.imageSetsByProvider = imageSetsByProvider;
    }

    public StackDetailsJson getStackDetails() {
        return stackDetails;
    }

    public void setStackDetails(StackDetailsJson stackDetails) {
        this.stackDetails = stackDetails;
    }
}
