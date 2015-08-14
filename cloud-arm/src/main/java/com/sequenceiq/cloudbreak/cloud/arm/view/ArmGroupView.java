package com.sequenceiq.cloudbreak.cloud.arm.view;

import java.util.ArrayList;
import java.util.List;

import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;

public class ArmGroupView {

    private List<Group> groups;

    private String stackName;

    public ArmGroupView(List<Group> groups, String stackName) {
        this.groups = groups;
    }

    public List<ArmInstanceView> getFlatArmView() {
        List<ArmInstanceView> armInstances = new ArrayList<>();
        for (Group group : groups) {
            for (InstanceTemplate instance : group.getInstances()) {
                ArmInstanceView novaInstance = new ArmInstanceView(instance, group.getType());
                armInstances.add(novaInstance);
            }
        }
        return armInstances;
    }

}