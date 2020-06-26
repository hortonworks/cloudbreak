package com.sequenceiq.cloudbreak.cloud.aws.view;

import java.util.List;
import java.util.Objects;

import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
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
        Object ev = instanceTemplate.getParameter("encrypted", Object.class);
        if (ev instanceof Boolean) {
            return (Boolean) ev;
        } else if (ev instanceof String) {
            return Boolean.parseBoolean((String) ev);
        }
        return false;
    }

    public boolean isKmsEnabled() {
        String type = instanceTemplate.getStringParameter("type");
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
        String type = instanceTemplate.getStringParameter("type");
        if (type != null) {
            EncryptionType ev = EncryptionType.valueOf(type);
            return ev == encryptionType;
        }
        return false;
    }

    public String getKmsKey() {
        return instanceTemplate.getStringParameter("key");
    }

    public Integer getSpotPercentage() {
        return instanceTemplate.getParameter("spotPercentage", Integer.class);
    }

    public int getOnDemandPercentage() {
        return HUNDRED_PERCENT - Objects.requireNonNullElse(getSpotPercentage(), 0);
    }
}
