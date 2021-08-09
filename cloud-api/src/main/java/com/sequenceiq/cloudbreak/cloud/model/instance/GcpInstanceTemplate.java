package com.sequenceiq.cloudbreak.cloud.model.instance;

import java.util.Collection;
import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;

public class GcpInstanceTemplate extends InstanceTemplate {
    /**
     * Key denoting the method  of the disk encryption set to be used for GCP resources. If set to KMS,
     * it would indicate the CMEK needs to be used. Default value would be RSA.GcpDiskResourceBuilder.java
     * <p>
     *     When set, the value shall be a nonempty {@String } containing the Key Encryption Method of the disk encryption.
     * </p>
     *
     * @see #putParameter(String, Object)
     */
    public static final String KEY_ENCRYPTION_METHOD = "keyEncryptionMethod";

    public GcpInstanceTemplate(String flavor, String groupName, Long privateId, Collection<Volume> volumes, InstanceStatus status,
            Map<String, Object> parameters, Long templateId, String imageId) {
        super(flavor, groupName, privateId, volumes, status, parameters, templateId, imageId, TemporaryStorage.ATTACHED_VOLUMES);
    }

}
