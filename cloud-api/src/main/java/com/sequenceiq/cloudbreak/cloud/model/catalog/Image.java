package com.sequenceiq.cloudbreak.cloud.model.catalog;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Image {

    private final String date;

    private final String description;

    private final String os;

    private final String osType;

    private final String uuid;

    private final String version;

    private final Map<String, String> repo;

    private final Map<String, Map<String, String>> imageSetsByProvider;

    private final StackDetails stackDetails;

    @JsonCreator
    public Image(
            @JsonProperty(value = "date", required = true) String date,
            @JsonProperty(value = "description", required = true) String description,
            @JsonProperty(value = "os", required = true) String os,
            @JsonProperty(value = "uuid", required = true) String uuid,
            @JsonProperty("version") String version,
            @JsonProperty("repo") Map<String, String> repo,
            @JsonProperty(value = "images", required = true) Map<String, Map<String, String>> imageSetsByProvider,
            @JsonProperty("stack-details") StackDetails stackDetails,
            @JsonProperty("os_type") String osType) {
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
