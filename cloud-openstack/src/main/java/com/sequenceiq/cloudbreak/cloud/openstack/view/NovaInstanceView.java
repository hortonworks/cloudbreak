package com.sequenceiq.cloudbreak.cloud.openstack.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackUtils;
import com.sequenceiq.cloudbreak.common.type.InstanceGroupType;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;

public class NovaInstanceView {

    private final String stackName;

    private final InstanceTemplate instance;

    private final InstanceGroupType type;

    private final Map<String, String> tags;

    private final String loginUserName;

    public NovaInstanceView(String stackName, InstanceTemplate instance, InstanceGroupType type, String loginUserName) {
        this.stackName = stackName;
        this.instance = instance;
        this.type = type;
        this.loginUserName = loginUserName;
        tags = Collections.emptyMap();
    }

    public NovaInstanceView(String stackName, InstanceTemplate instance, InstanceGroupType type, Map<String, String> tags, String loginUserName) {
        this.stackName = stackName;
        this.instance = instance;
        this.type = type;
        this.tags = tags;
        this.loginUserName = loginUserName;
    }

    public String getFlavor() {
        return instance.getFlavor();
    }

    public InstanceGroupType getType() {
        return type;
    }

    public String getInstanceId() {
        return instance.getGroupName().replaceAll("[_-]", "") + '_' + instance.getPrivateId();
    }

    public String getName() {
        String stackName = this.stackName.replaceAll("_", "-");
        String shortenedGroupName = instance.getGroupName().replaceAll("host|group|[_-]", "").trim();
        return stackName + '-' + shortenedGroupName + '-' + instance.getPrivateId();
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
        } catch (JsonProcessingException ignored) {
            return generateMetadata().toString();
        }
    }

    public InstanceTemplate getInstance() {
        return instance;
    }

    private Map<String, String> generateMetadata() {
        Map<String, String> metadata = new HashMap<>(tags);
        metadata.put(OpenStackUtils.CB_INSTANCE_GROUP_NAME, instance.getGroupName());
        metadata.put(OpenStackUtils.CB_INSTANCE_PRIVATE_ID, Long.toString(getPrivateId()));
        return metadata;
    }

    public String getLoginUserName() {
        return loginUserName;
    }

    public String getImageId() {
        return instance.getImageId();
    }
}
