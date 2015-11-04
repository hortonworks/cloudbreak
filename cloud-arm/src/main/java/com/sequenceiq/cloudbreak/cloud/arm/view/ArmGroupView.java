package com.sequenceiq.cloudbreak.cloud.arm.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.Group;

public class ArmGroupView {

    private Map<String, List<ArmInstanceView>> groups = new HashMap<>();

    public ArmGroupView(List<Group> groupList) {
        for (Group group : groupList) {
            String groupName = group.getType().name();
            List<ArmInstanceView> existingInstances = groups.get(groupName);
            if (existingInstances == null) {
                existingInstances = new ArrayList<>();
                groups.put(groupName, existingInstances);
            }
            for (CloudInstance instance : group.getInstances()) {
                ArmInstanceView novaInstance = new ArmInstanceView(instance.getTemplate(), group.getType());
                existingInstances.add(novaInstance);
            }
        }
    }

    public Map<String, List<ArmInstanceView>> getGroups() {
        return groups;
    }
}