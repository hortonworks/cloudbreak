package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

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

    private final long created;

    private final String date;

    private final String description;

    private final String os;

    private final String osType;

    private final String uuid;

    private final Map<String, Map<String, String>> imageSetsByProvider;

    private final Map<String, String> packageVersions;

    private boolean advertised;

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
            @JsonProperty(ADVERTISED_PROPERTY) boolean advertised) {
        this.created = Objects.requireNonNullElse(created, 0L);
        this.date = date;
        this.description = description;
        this.os = os;
        this.osType = osType;
        this.uuid = uuid;
        this.imageSetsByProvider = imageSetsByProvider;
        this.packageVersions = packageVersions;
        this.advertised = advertised;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Image image = (Image) o;
        return Objects.equals(date, image.date)
                && Objects.equals(description, image.description)
                && Objects.equals(os, image.os)
                && Objects.equals(osType, image.osType)
                && Objects.equals(uuid, image.uuid)
                && Objects.equals(imageSetsByProvider, image.imageSetsByProvider)
                && Objects.equals(advertised, image.advertised);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, description, os, osType, uuid, imageSetsByProvider, advertised);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("Image{");
        sb.append("date='").append(date).append('\'');
        sb.append(", description='").append(description).append('\'');
        sb.append(", os='").append(os).append('\'');
        sb.append(", osType='").append(osType).append('\'');
        sb.append(", uuid='").append(uuid).append('\'');
        sb.append(", imageSetsByProvider=").append(imageSetsByProvider);
        sb.append(", advertised=").append(advertised);
        sb.append('}');
        return sb.toString();
    }
}
