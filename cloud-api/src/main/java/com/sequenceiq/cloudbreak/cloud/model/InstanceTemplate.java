package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.collections4.CollectionUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.common.api.type.EncryptionType;

public class InstanceTemplate extends DynamicModel {

    /**
     * Key of the optional dynamic parameter denoting the type of encryption key to use for disk volume encryption. The exact interpretation of this setting
     * is up to the target cloud provider.
     *
     * <p>
     *     Permitted values are the names of enum constants in {@link EncryptionType}:
     *     <ul>
     *         <li>{@code "NONE"}, {@code null}: No encryption key. This setting essentially disables volume encryption.</li>
     *         <li>{@code "DEFAULT"}: Use the default key. This is typically a cloud provider managed encryption key, but can also mean some customer-managed
     *         key designated as the "default" in a cloud provider specific way.</li>
     *         <li>{@code "CUSTOM"}: Use the customer-managed encryption key specified in {@link #VOLUME_ENCRYPTION_KEY_ID}.</li>
     *     </ul>
     * </p>
     *
     * @see #VOLUME_ENCRYPTION_KEY_ID
     * @see #putParameter(String, Object)
     * @see EncryptionType
     */
    public static final String VOLUME_ENCRYPTION_KEY_TYPE = "type";

    /**
     * Key of the dynamic parameter denoting the ID of the customer-managed encryption key to be used for disk volume encryption. Relevant and required only
     * when {@link #VOLUME_ENCRYPTION_KEY_TYPE} equals {@code "CUSTOM"}. Its value will be ignored otherwise.
     *
     * <p>
     *     When set, the value shall be a nonempty {@link String} containing the ID of the customer-managed encryption key in a cloud provider specific syntax.
     * </p>
     *
     * @see #VOLUME_ENCRYPTION_KEY_TYPE
     * @see #putParameter(String, Object)
     */
    public static final String VOLUME_ENCRYPTION_KEY_ID = "key";

    private String flavor;

    private String groupName;

    private Long privateId;

    private List<Volume> volumes;

    private InstanceStatus status;

    private Long templateId;

    private String imageId;

    private TemporaryStorage temporaryStorage;

    private Long temporaryStorageCount;

    @JsonCreator
    public InstanceTemplate(@JsonProperty("flavor") String flavor,
            @JsonProperty("groupName") String groupName,
            @JsonProperty("privateId") Long privateId,
            @JsonProperty("volumes") Collection<Volume> volumes,
            @JsonProperty("status") InstanceStatus status,
            @JsonProperty("parameters") Map<String, Object> parameters,
            @JsonProperty("templateId") Long templateId,
            @JsonProperty("imageId") String imageId,
            @JsonProperty("temporaryStorage") TemporaryStorage temporaryStorage,
            @JsonProperty("temporaryStorageCount") Long temporaryStorageCount) {
        super(parameters);
        this.flavor = flavor;
        this.templateId = templateId;
        this.groupName = groupName;
        this.privateId = privateId;
        this.volumes = ImmutableList.copyOf(CollectionUtils.emptyIfNull(volumes));
        this.status = status;
        this.imageId = imageId;
        this.temporaryStorage = temporaryStorage;
        this.temporaryStorageCount = temporaryStorageCount;
    }

    public String getFlavor() {
        return flavor;
    }

    public List<Volume> getVolumes() {
        return volumes;
    }

    public String getGroupName() {
        return groupName;
    }

    public Long getPrivateId() {
        return privateId;
    }

    public InstanceStatus getStatus() {
        return status;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public String getImageId() {
        return imageId;
    }

    public TemporaryStorage getTemporaryStorage() {
        return temporaryStorage;
    }

    public Long getTemporaryStorageCount() {
        return temporaryStorageCount;
    }

    public void setFlavor(String flavor) {
        this.flavor = flavor;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public void setPrivateId(Long privateId) {
        this.privateId = privateId;
    }

    public void setVolumes(List<Volume> volumes) {
        this.volumes = volumes;
    }

    public void setStatus(InstanceStatus status) {
        this.status = status;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public void setTemporaryStorage(TemporaryStorage temporaryStorage) {
        this.temporaryStorage = temporaryStorage;
    }

    public void setTemporaryStorageCount(Long temporaryStorageCount) {
        this.temporaryStorageCount = temporaryStorageCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        InstanceTemplate that = (InstanceTemplate) o;
        return Objects.equals(flavor, that.flavor)
                && Objects.equals(groupName, that.groupName)
                && Objects.equals(privateId, that.privateId)
                && Objects.equals(volumes, that.volumes)
                && status == that.status
                && Objects.equals(templateId, that.templateId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(flavor, groupName, privateId, volumes, status, templateId);
    }

    @Override
    public String toString() {
        return "InstanceTemplate{" +
                "flavor='" + flavor + '\'' +
                ", groupName='" + groupName + '\'' +
                ", privateId=" + privateId +
                ", volumes=" + volumes +
                ", status=" + status +
                ", templateId=" + templateId +
                ", imageId='" + imageId + '\'' +
                ", temporaryStorage=" + temporaryStorage +
                ", temporaryStorageCount=" + temporaryStorageCount +
                '}';
    }
}
