package com.sequenceiq.cloudbreak.cloud.aws.common.view;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.cloud.model.instance.AwsInstanceTemplate;
import com.sequenceiq.common.api.placement.AwsPlacementGroupStrategy;
import com.sequenceiq.common.api.type.EncryptionType;

public class AwsInstanceView {

    private static final int HUNDRED_PERCENT = 100;

    private final InstanceTemplate instanceTemplate;

    public AwsInstanceView(InstanceTemplate instanceTemplate) {
        this.instanceTemplate = instanceTemplate;
    }

    public List<Volume> getVolumes() {
        return instanceTemplate.getVolumes();
    }

    public Long getTemporaryStorageCount() {
        return instanceTemplate.getTemporaryStorageCount();
    }

    public String getFlavor() {
        return instanceTemplate.getFlavor();
    }

    public String getGroupName() {
        return instanceTemplate.getGroupName();
    }

    public Long getPrivateId() {
        return instanceTemplate.getPrivateId();
    }

    public Long getTemplateId() {
        return instanceTemplate.getTemplateId();
    }

    public boolean isEncryptedVolumes() {
        Object ev = instanceTemplate.getParameter(AwsInstanceTemplate.EBS_ENCRYPTION_ENABLED, Object.class);
        if (ev instanceof Boolean) {
            return (Boolean) ev;
        } else if (ev instanceof String) {
            return Boolean.parseBoolean((String) ev);
        }
        return false;
    }

    public boolean isKmsEnabled() {
        String type = instanceTemplate.getStringParameter(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE);
        if (type != null) {
            EncryptionType ev = EncryptionType.valueOf(type);
            return ev != EncryptionType.NONE;
        }
        return false;
    }

    public boolean isKmsDefault() {
        return isTypeEqualsWith(EncryptionType.DEFAULT);
    }

    public boolean isKmsCustom() {
        return isTypeEqualsWith(EncryptionType.CUSTOM);
    }

    private boolean isTypeEqualsWith(EncryptionType encryptionType) {
        String type = instanceTemplate.getStringParameter(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE);
        if (type != null) {
            EncryptionType ev = EncryptionType.valueOf(type);
            return ev == encryptionType;
        }
        return false;
    }

    public String getKmsKey() {
        return instanceTemplate.getStringParameter(InstanceTemplate.VOLUME_ENCRYPTION_KEY_ID);
    }

    public Integer getSpotPercentage() {
        return instanceTemplate.getParameter(AwsInstanceTemplate.EC2_SPOT_PERCENTAGE, Integer.class);
    }

    public AwsPlacementGroupStrategy getPlacementGroupStrategy() {
        return Optional.ofNullable(instanceTemplate.getStringParameter(AwsInstanceTemplate.PLACEMENT_GROUP_STRATEGY))
                .map(placement -> AwsPlacementGroupStrategy.valueOf(placement))
                .orElse(AwsPlacementGroupStrategy.NONE);
    }

    public Double getSpotMaxPrice() {
        return instanceTemplate.getParameter(AwsInstanceTemplate.EC2_SPOT_MAX_PRICE, Double.class);
    }

    public int getOnDemandPercentage() {
        return HUNDRED_PERCENT - Objects.requireNonNullElse(getSpotPercentage(), 0);
    }

}
