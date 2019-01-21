package com.sequenceiq.cloudbreak.cloud.aws.view;

import java.util.List;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.EncryptionType;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Volume;

public class AwsInstanceView {

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

    public String getVolumeType() {
        return instanceTemplate.getVolumeType();
    }

    public int getVolumeSize() {
        return instanceTemplate.getVolumeSize();
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

    public Double getSpotPrice() {
        Object sv = instanceTemplate.getParameter("spotPrice", Object.class);
        if (sv instanceof Double) {
            return (Double) sv;
        } else if (sv instanceof Integer) {
            return ((Number) sv).doubleValue();
        } else if (sv instanceof String) {
            return Double.valueOf((String) sv);
        }
        return null;
    }
}