package com.sequenceiq.cloudbreak.cloud.openstack.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.cloud.model.Instance;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.cloud.openstack.OpenStackHeatUtils;
import com.sequenceiq.cloudbreak.domain.InstanceGroupType;

public class NovaInstanceView {

    private Instance instance;

    private InstanceGroupType type;

    public NovaInstanceView(Instance instance, InstanceGroupType type) {
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
        return instance.getGroupName().replaceAll("_", "") + "_" + instance.getPrivateId();
    }

    public int getPrivateId() {
        return instance.getPrivateId();
    }

    public List<CinderVolumeView> getVolumes() {
        List<CinderVolumeView> list = new ArrayList<>();
        int index = 0;
        for (Volume volume : instance.getVolumes()) {
            CinderVolumeView cv = new CinderVolumeView(volume, index);
            list.add(cv);
            index++;
        }
        return list;
    }

    public String getMetadata() {
        try {
            return new ObjectMapper().writeValueAsString(generateMetadata());
        } catch (JsonProcessingException e) {
            return generateMetadata().toString();
        }
    }

    private Map<String, String> generateMetadata() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put(OpenStackHeatUtils.CB_INSTANCE_GROUP_NAME, instance.getGroupName());
        metadata.put(OpenStackHeatUtils.CB_INSTANCE_PRIVATE_ID, Integer.toString(instance.getPrivateId()));
        return metadata;
    }

}
