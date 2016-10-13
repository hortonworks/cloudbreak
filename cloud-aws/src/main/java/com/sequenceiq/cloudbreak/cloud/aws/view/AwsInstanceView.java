package com.sequenceiq.cloudbreak.cloud.aws.view;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Volume;

public class AwsInstanceView {


    private InstanceTemplate instanceTemplate;

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

    public boolean isEncryptedVolumes() {
        Object ev = instanceTemplate.getParameter("encrypted", Object.class);
        if (ev instanceof Boolean) {
            return ((Boolean) ev).booleanValue();
        }
        if (ev instanceof String) {
            return Boolean.parseBoolean((String) ev);
        }
        return false;
    }

    public Double getSpotPrice() {
        Object sv = instanceTemplate.getParameter("spotPrice", Object.class);
        if (sv instanceof Double) {
            return (Double) sv;
        }
        if (sv instanceof Integer) {
            return ((Integer) sv).doubleValue();
        }
        if (sv instanceof String) {
            return Double.parseDouble((String) sv);
        }
        return null;
    }
}