package com.sequenceiq.cloudbreak.cloud.openstack.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupType;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackUtils;
import com.sequenceiq.cloudbreak.util.JsonUtil;

public class NovaInstanceView {

    private InstanceTemplate instance;

    private InstanceGroupType type;

    public NovaInstanceView(InstanceTemplate instance, InstanceGroupType type) {
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

    public long getPrivateId() {
        return instance.getPrivateId();
    }

    public int getVolumesCount() {
        if (instance.getVolumes() == null) {
            return 0;
        }
        return instance.getVolumes().size();
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

    public Map<String, String> getMetadataMap() {
        return generateMetadata();
    }

    public String getMetadata() {
        try {
            return JsonUtil.writeValueAsString(generateMetadata());
        } catch (JsonProcessingException e) {
            return generateMetadata().toString();
        }
    }

    public InstanceTemplate getInstance() {
        return instance;
    }

    private Map<String, String> generateMetadata() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put(OpenStackUtils.CB_INSTANCE_GROUP_NAME, instance.getGroupName());
        metadata.put(OpenStackUtils.CB_INSTANCE_PRIVATE_ID, Long.toString(getPrivateId()));
        return metadata;
    }

}
