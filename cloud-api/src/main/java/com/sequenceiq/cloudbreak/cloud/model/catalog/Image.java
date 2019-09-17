package com.sequenceiq.cloudbreak.cloud.model.catalog;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    private boolean defaultImage;

    private final Map<String, String> packageVersions;

    private final List<List<String>> preWarmParcels;

    private final List<String> preWarmCsd;

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
            @JsonProperty("os_type") String osType,
            @JsonProperty("package-versions") Map<String, String> packageVersions,
            @JsonProperty("pre_warm_parcels") List<List<String>> preWarmParcels,
            @JsonProperty("pre_warm_csd") List<String> preWarmCsd) {
        this.date = date;
        this.description = description;
        this.os = os;
        this.uuid = uuid;
        this.version = version;
        this.repo = (repo == null) ? Collections.emptyMap() : repo;
        this.imageSetsByProvider = imageSetsByProvider;
        this.stackDetails = stackDetails;
        this.osType = osType;
        this.packageVersions = packageVersions;
        this.preWarmParcels = preWarmParcels == null ? Collections.emptyList() : preWarmParcels;
        this.preWarmCsd = preWarmCsd == null ? Collections.emptyList() : preWarmCsd;
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

    public void setDefaultImage(boolean defaultImage) {
        this.defaultImage = defaultImage;
    }

    public boolean isDefaultImage() {
        return defaultImage;
    }

    public Map<String, String> getPackageVersions() {
        return packageVersions == null ? new HashMap<>() : packageVersions;
    }

    public boolean isPrewarmed() {
        return stackDetails != null && stackDetails.getRepo() != null && stackDetails.getRepo().getStack() != null;
    }

    public List<List<String>> getPreWarmParcels() {
        return preWarmParcels;
    }

    public List<String> getPreWarmCsd() {
        return preWarmCsd;
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
                + ", default='" + defaultImage + '\''
                + ", packageVersions='" + packageVersions + '\''
                + ", preWarmParcels='" + preWarmParcels.stream().flatMap(Collection::stream).collect(Collectors.joining(", ")) + '\''
                + ", preWarmCsd='" + String.join(", ", preWarmCsd) + '\''
                + '}';
    }

    public String shortOsDescriptionFormat() {
        return "Image{"
                + "uuid='" + uuid + '\''
                + ", os='" + os + '\''
                + '}';
    }
}
