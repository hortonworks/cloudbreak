package com.sequenceiq.cloudbreak.cloud.model.instance;

import java.util.Collection;
import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;

public class GcpInstanceTemplate extends InstanceTemplate {
    /**
     * Key denoting the method  of the disk encryption method to be used for GCP resources. If set to KMS,
     * it would indicate the CMEK needs to be used. Default value would be RSA.
     * <p>
     *     Permitted values:
     *     <ul>
     *         <li>{@ENUM RAW} </li>
     *         <li>{@ENUM KMS}: {@link #VOLUME_ENCRYPTION_KEY_ID}  will be used to encrypt the resource. </li>
     *         <li>{@ENUM RSA}: Default Google managed encryption of resources. </li>
     *         <li>{@code null}: RSA wil be considered as default value if null is provided. </li>
     *     </ul>
     * </p>
     */
    public static final String KEY_ENCRYPTION_METHOD = "keyEncryptionMethod";

    public GcpInstanceTemplate(String flavor, String groupName, Long privateId, Collection<Volume> volumes, InstanceStatus status,
            Map<String, Object> parameters, Long templateId, String imageId) {
        super(flavor, groupName, privateId, volumes, status, parameters, templateId, imageId, TemporaryStorage.ATTACHED_VOLUMES, 0L);
    }
}