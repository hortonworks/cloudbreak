package com.sequenceiq.cloudbreak.cloud.model.catalog;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Image {

    private String date;

    private String description;

    private String os;

    private String osType;

    private String uuid;

    private String version;

    private Map<String, String> repo;

    private Map<String, Map<String, String>> imageSetsByProvider;

    private StackDetails stackDetails;

    @JsonCreator
    public Image(
            @JsonProperty(value = "date", required = true) String date,
            @JsonProperty(value = "description", required = true) String description,
            @JsonProperty(value = "os", required = true) String os,
            @JsonProperty(value = "uuid", required = true) String uuid,
            @JsonProperty(value = "version") String version,
            @JsonProperty(value = "repo") Map<String, String> repo,
            @JsonProperty(value = "images", required = true) Map<String, Map<String, String>> imageSetsByProvider,
            @JsonProperty(value = "stack-details") StackDetails stackDetails,
            @JsonProperty(value = "os_type") String osType) {
        this.date = date;
        this.description = description;
        this.os = os;
        this.uuid = uuid;
        this.version = version;
        this.repo = (repo == null) ? Collections.emptyMap() : repo;
        this.imageSetsByProvider = imageSetsByProvider;
        this.stackDetails = stackDetails;
        this.osType = osType;
    }

    public String getDate() {
        return date;
    }

    public String getDescription() {
        return description;
    }

    public String getOs() {
        return os;
    }

    public String getUuid() {
        return uuid;
    }

    public String getVersion() {
        return version;
    }

    public Map<String, String> getRepo() {
        return repo;
    }

    public Map<String, Map<String, String>> getImageSetsByProvider() {
        return imageSetsByProvider;
    }

    public StackDetails getStackDetails() {
        return stackDetails;
    }

    public String getOsType() {
        return osType;
    }

    @Override
    public String toString() {
        return "Image{"
                + "uuid='" + uuid + '\''
                + ", date='" + date + '\''
                + ", description='" + description + '\''
                + ", os='" + os + '\''
                + ", osType='" + osType + '\''
                + ", version='" + version + '\''
                + '}';
    }
}
