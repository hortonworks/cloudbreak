package com.sequenceiq.cloudbreak.cloud.model.instance;

import java.util.Collection;
import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Volume;

public class AzureInstanceTemplate extends InstanceTemplate {

    /**
     * Key of the optional dynamic parameter denoting whether managed disk encryption with a customer-managed encryption key is enabled or not. This applies
     * to both root & attached (data) volumes.
     *
     * <p>
     *     Permitted values:
     *     <ul>
     *         <li>{@code Boolean.TRUE} instance, {@code "true"} (ignoring case): Encryption with a customer-managed encryption key is enabled.</li>
     *         <li>{@code Boolean.FALSE} instance, {@code "false"} (or any other {@code String} not equal to {@code "true"} ignoring case), {@code null}:
     *         Encryption with a customer-managed encryption key is disabled. This implies that managed disks will be subject to the default Storage Service
     *         Encryption with a platform managed key.</li>
     *     </ul>
     * </p>
     *
     * If enabled, the encryption key to use is determined by the disk encryption set specified by {@link #DISK_ENCRYPTION_SET_ID}. In particular,
     * {@link #VOLUME_ENCRYPTION_KEY_TYPE} and {@link #VOLUME_ENCRYPTION_KEY_ID} are currently ignored for managed disk encryption.
     *
     * @see #DISK_ENCRYPTION_SET_ID
     * @see #VOLUME_ENCRYPTION_KEY_ID
     * @see #VOLUME_ENCRYPTION_KEY_TYPE
     * @see #putParameter(String, Object)
     * @see Boolean#parseBoolean(String)
     */
    public static final String MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED = "encryptionWithCustomKey";

    /**
     * Key of the dynamic parameter denoting the Resource ID of the disk encryption set to be used for managed disk encryption. Relevant and required only
     * when {@link #MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED} is {@code true}. Its value will be ignored otherwise.
     *
     * <p>
     *     When set, the value shall be a nonempty {@link String} containing the Resource ID of the disk encryption set.
     * </p>
     *
     * @see #MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED
     * @see #putParameter(String, Object)
     */
    public static final String DISK_ENCRYPTION_SET_ID = "diskEncryptionSetId";

    public AzureInstanceTemplate(String flavor, String groupName, Long privateId, Collection<Volume> volumes, InstanceStatus status,
            Map<String, Object> parameters, Long templateId, String imageId) {
        super(flavor, groupName, privateId, volumes, status, parameters, templateId, imageId);
    }

}
