package com.sequenceiq.cloudbreak.cloud.arm.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.common.type.InstanceGroupType;
import com.sequenceiq.cloudbreak.util.JsonUtil;

public class ArmInstanceView {

    private InstanceTemplate instance;

    private InstanceGroupType type;

    public ArmInstanceView(InstanceTemplate instance, InstanceGroupType type) {
        this.instance = instance;
        this.type = type;
    }

    public String getFlavor() {
        return instance.getFlavor();
    }

    public InstanceGroupType getType() {
        return type;
    }

    public String getInstanceId() {
        return instance.getGroupName().replaceAll("_", "") + instance.getPrivateId();
    }

    public long getPrivateId() {
        return instance.getPrivateId();
    }

    public List<ArmVolumeView> getVolumes() {
        List<ArmVolumeView> list = new ArrayList<>();
        int index = 0;
        for (Volume volume : instance.getVolumes()) {
            ArmVolumeView cv = new ArmVolumeView(volume, index);
            list.add(cv);
            index++;
        }
        return list;
    }

    public String getMetadata() {
        try {
            return JsonUtil.writeValueAsString(generateMetadata());
        } catch (JsonProcessingException e) {
            return generateMetadata().toString();
        }
    }

    private Map<String, String> generateMetadata() {
        return new HashMap<>();
    }
}