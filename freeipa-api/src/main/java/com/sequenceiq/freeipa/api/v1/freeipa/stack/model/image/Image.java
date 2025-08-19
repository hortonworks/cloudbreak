package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Image {

    private static final String OS_TYPE_PROPERTY = "os_type";

    private static final String IMAGES_PROPERTY = "images";

    private static final String PACKAGE_VERSIONS_PROPERTY = "package-versions";

    private static final String ADVERTISED_PROPERTY = "advertised";

    private static final String CREATED_PROPERTY = "created";

    private static final String DATE_PROPERTY = "date";

    private static final String DESCRIPTION_PROPERTY = "description";

    private static final String OS_PROPERTY = "os";

    private static final String UUID_PROPERTY = "uuid";

    private static final String ARCHITECTURE_PROPERTY = "architecture";

    private static final String TAGS_PROPERTY = "tags";

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private final long created;

    private final String date;

    private final String description;

    private final String os;

    private final String osType;

    private final String uuid;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private final Map<String, Map<String, String>> imageSetsByProvider;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private final Map<String, String> packageVersions;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private final boolean advertised;

    private final String architecture;

    private final Map<String, String> tags;

    @JsonCreator
    public Image(
            @JsonProperty(CREATED_PROPERTY) Long created,
            @JsonProperty(value = DATE_PROPERTY, required = true) String date,
            @JsonProperty(value = DESCRIPTION_PROPERTY, required = true) String description,
            @JsonProperty(value = OS_PROPERTY, required = true) String os,
            @JsonProperty(value = UUID_PROPERTY, required = true) String uuid,
            @JsonProperty(value = IMAGES_PROPERTY, required = true) Map<String, Map<String, String>> imageSetsByProvider,
            @JsonProperty(OS_TYPE_PROPERTY) String osType,
            @JsonProperty(PACKAGE_VERSIONS_PROPERTY) Map<String, String> packageVersions,
            @JsonProperty(ADVERTISED_PROPERTY) boolean advertised,
            @JsonProperty(ARCHITECTURE_PROPERTY) String architecture,
            @JsonProperty(TAGS_PROPERTY) Map<String, String> tags) {
        this.created = Objects.requireNonNullElse(created, 0L);
        this.date = date;
        this.description = description;
        this.os = os;
        this.osType = osType;
        this.uuid = uuid;
        this.imageSetsByProvider = imageSetsByProvider;
        this.packageVersions = packageVersions;
        this.advertised = advertised;
        this.architecture = architecture;
        this.tags = tags == null ? Map.of() : tags;
    }

    public Image(Long created, String date, String description, String os, String uuid, Map<String, Map<String, String>> imageSetsByProvider, String osType,
            Map<String, String> packageVersions, boolean advertised, String architecture) {
        this.created = Objects.requireNonNullElse(created, 0L);
        this.date = date;
        this.description = description;
        this.os = os;
        this.osType = osType;
        this.uuid = uuid;
        this.imageSetsByProvider = imageSetsByProvider;
        this.packageVersions = packageVersions;
        this.advertised = advertised;
        this.architecture = architecture;
        this.tags = Map.of();
    }

    @JsonProperty(CREATED_PROPERTY)
    public long getCreated() {
        return created;
    }

    @JsonProperty(DATE_PROPERTY)
    public String getDate() {
        return date;
    }

    @JsonProperty(DESCRIPTION_PROPERTY)
    public String getDescription() {
        return description;
    }

    @JsonProperty(OS_PROPERTY)
    public String getOs() {
        return os;
    }

    @JsonProperty(UUID_PROPERTY)
    public String getUuid() {
        return uuid;
    }

    @JsonProperty(OS_TYPE_PROPERTY)
    public String getOsType() {
        return osType;
    }

    @JsonProperty(IMAGES_PROPERTY)
    public Map<String, Map<String, String>> getImageSetsByProvider() {
        return imageSetsByProvider;
    }

    @JsonProperty(PACKAGE_VERSIONS_PROPERTY)
    public Map<String, String> getPackageVersions() {
        return packageVersions;
    }

    @JsonProperty(ADVERTISED_PROPERTY)
    public boolean isAdvertised() {
        return advertised;
    }

    @JsonProperty(ARCHITECTURE_PROPERTY)
    public String getArchitecture() {
        return architecture;
    }

    @JsonProperty(TAGS_PROPERTY)
    public Map<String, String> getTags() {
        return tags;
    }

    // CHECKSTYLE:OFF
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Image image = (Image) o;
        return created == image.created
                && advertised == image.advertised
                && Objects.equals(date, image.date)
                && Objects.equals(description, image.description)
                && Objects.equals(os, image.os)
                && Objects.equals(osType, image.osType)
                && Objects.equals(uuid, image.uuid)
                && Objects.equals(imageSetsByProvider, image.imageSetsByProvider)
                && Objects.equals(packageVersions, image.packageVersions)
                && Objects.equals(architecture, image.architecture)
                && Objects.equals(tags, image.tags);
    }
    // CHECKSTYLE:ON

    @Override
    public int hashCode() {
        return Objects.hash(created, date, description, os, osType, uuid, imageSetsByProvider,
                packageVersions, advertised, architecture, tags);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("Image{");
        sb.append("created=").append(created);
        sb.append(", date='").append(date).append('\'');
        sb.append(", description='").append(description).append('\'');
        sb.append(", os='").append(os).append('\'');
        sb.append(", osType='").append(osType).append('\'');
        sb.append(", uuid='").append(uuid).append('\'');
        sb.append(", imageSetsByProvider=").append(imageSetsByProvider);
        sb.append(", packageVersions=").append(packageVersions);
        sb.append(", advertised=").append(advertised);
        sb.append(", architecture='").append(architecture).append('\'');
        sb.append(", tags=").append(tags);
        sb.append('}');
        return sb.toString();
    }
}
