package com.sequenceiq.cloudbreak.cloud.openstack.view;

import java.util.ArrayList;
import java.util.List;

import com.sequenceiq.cloudbreak.cloud.model.Instance;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.domain.InstanceGroupType;

public class NovaInstanceView {

    private Instance instance;

    private InstanceGroupType type;

    private String groupName;

    private int privateId;

    public NovaInstanceView(Instance instance, InstanceGroupType type, String groupName, int privateId) {
        this.instance = instance;
        this.type = type;
        this.groupName = groupName;
        this.privateId = privateId;
    }

    public String getFlavor() {
        return instance.getFlavor();
    }

    public InstanceGroupType getType() {
        return type;
    }

    public String getInstanceId() {
        return groupName.replaceAll("_", "") + "_" + privateId;
    }

    public int getPrivateId() {
        return privateId;
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

}
